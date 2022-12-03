/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.Query
import org.rust.ide.icons.RsIcons
import org.rust.lang.core.macros.RsExpandedElement
import org.rust.lang.core.psi.*
import org.rust.lang.core.resolve.KNOWN_DERIVABLE_TRAITS
import org.rust.lang.core.resolve.knownItems
import org.rust.lang.core.resolve.ref.RsMacroBodyReferenceDelegateImpl
import org.rust.lang.core.stubs.RsTraitItemStub
import org.rust.lang.core.types.*
import org.rust.lang.core.types.consts.CtConstParameter
import org.rust.lang.core.types.infer.substitute
import org.rust.lang.core.types.regions.ReEarlyBound
import org.rust.lang.core.types.ty.Ty
import org.rust.lang.core.types.ty.TyTypeParameter
import org.rust.openapiext.filterIsInstanceQuery
import org.rust.openapiext.filterQuery
import org.rust.openapiext.mapQuery
import javax.swing.Icon

val RsTraitItem.langAttribute: String? get() = queryAttributes.langAttribute

val RsTraitItem.isSizedTrait: Boolean get() = langAttribute == "sized"

val RsTraitItem.isAuto: Boolean
    get() = greenStub?.isAuto ?: (node.findChildByType(RsElementTypes.AUTO) != null)

val RsTraitItem.isKnownDerivable: Boolean
    get() {
        val derivableTrait = KNOWN_DERIVABLE_TRAITS[name] ?: return false
        if (!derivableTrait.shouldUseHardcodedTraitDerive()) return false
        return derivableTrait.findTrait(knownItems) == this
    }

fun BoundElement<RsTraitItem>.getFlattenHierarchy(selfTy: Ty? = null): Collection<BoundElement<RsTraitItem>> {
    val result = mutableListOf<BoundElement<RsTraitItem>>()
    val visited = mutableSetOf<RsTraitItem>()
    fun dfs(boundTrait: BoundElement<RsTraitItem>) {
        if (!visited.add(boundTrait.element)) return
        result += boundTrait
        boundTrait.element.superTraits.forEach { rawSuperTrait ->
            val superTrait = if (selfTy != null) {
                rawSuperTrait.substitute(mapOf(TyTypeParameter.self() to selfTy).toTypeSubst())
            } else {
                rawSuperTrait
            }
            run {
                // infer associated types on supertraits if possible
                if (boundTrait.assoc.isNotEmpty()) {
                    val inferredAssoc = mutableMapOf<RsTypeAlias, Ty>()
                    val superTraitAssocTypes = superTrait.element.expandedMembers.types
                    boundTrait.assoc.filterTo(inferredAssoc) { it.key in superTraitAssocTypes }
                    if (inferredAssoc.isNotEmpty()) {
                        inferredAssoc.putAll(superTrait.assoc)
                        return@run superTrait.copy(assoc = inferredAssoc)
                    }
                }
                superTrait
            }.let {
                dfs(it.substitute(boundTrait.subst))
            }
        }
    }
    dfs(this)

    return result
}

val BoundElement<RsTraitItem>.associatedTypesTransitively: Collection<RsTypeAlias>
    get() = getFlattenHierarchy().flatMap { it.element.expandedMembers.types }

fun RsTraitItem.findAssociatedType(name: String): RsTypeAlias? =
    associatedTypesTransitively.find { it.name == name }

fun RsTraitItem.substAssocType(assocName: String, ty: Ty?): BoundElement<RsTraitItem> =
    BoundElement(this).substAssocType(assocName, ty)

fun BoundElement<RsTraitItem>.substAssocType(assocName: String, ty: Ty?): BoundElement<RsTraitItem> {
    val assocType = element.findAssociatedType(assocName)
    val assoc = if (assocType != null && ty != null) assoc + (assocType to ty) else assoc
    return BoundElement(element, subst, assoc)
}

fun RsTraitItem.searchForImplementations(): Query<RsImplItem> {
    @Suppress("UnstableApiUsage")
    return ReferencesSearch.search(this, this.useScope)
        .transforming {
            if (it is RsMacroBodyReferenceDelegateImpl) {
                it.expandedDelegates
            } else {
                listOf(it)
            }
        }
        .mapQuery { it.element.parent?.parent }
        .filterIsInstanceQuery<RsImplItem>()
        .filterQuery { it.typeReference != null }
}

private val RsTraitItem.superTraits: Sequence<BoundElement<RsTraitItem>>
    get() {
        // trait Foo where Self: Bar {}
        val whereBounds = whereClause?.wherePredList.orEmpty().asSequence()
            .filter { (it.typeReference?.skipParens() as? RsPathType)?.path?.hasCself == true }
            .flatMap { it.typeParamBounds?.polyboundList.orEmpty().asSequence() }
        // trait Foo: Bar {}
        val bounds = typeParamBounds?.polyboundList.orEmpty().asSequence() + whereBounds
        return bounds
            .filter { !it.hasQ } // ignore `?Sized`
            .mapNotNull { it.bound.traitRef?.resolveToBoundTrait() }
    }

fun RsTraitItem.withDefaultSubst(): BoundElement<RsTraitItem> =
    BoundElement(this, defaultSubstitution(this))

private fun defaultSubstitution(item: RsTraitItem): Substitution {
    val typeSubst = item.typeParameters.associate {
        val parameter = TyTypeParameter.named(it)
        parameter to parameter
    }
    val regionSubst = item.lifetimeParameters.associate {
        val parameter = ReEarlyBound(it)
        parameter to parameter
    }
    val constSubst = item.constParameters.associate {
        val parameter = CtConstParameter(it)
        parameter to parameter
    }
    return Substitution(typeSubst, regionSubst, constSubst)
}

abstract class RsTraitItemImplMixin : RsStubbedNamedElementImpl<RsTraitItemStub>, RsTraitItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: RsTraitItemStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int): Icon =
        iconWithVisibility(flags, RsIcons.TRAIT)

    override val crateRelativePath: String? get() = RsPsiImplUtil.crateRelativePath(this)

    override val implementedTrait: BoundElement<RsTraitItem>? get() = BoundElement(this)

    override val associatedTypesTransitively: Collection<RsTypeAlias>
        get() = BoundElement(this).associatedTypesTransitively

    override val isUnsafe: Boolean
        get() {
            val stub = greenStub
            return stub?.isUnsafe ?: (unsafe != null)
        }

    override val declaredType: Ty get() = RsPsiTypeImplUtil.declaredType(this)

    override fun getContext(): PsiElement? = RsExpandedElement.getContextImpl(this)

    override fun getUseScope(): SearchScope = RsPsiImplUtil.getDeclarationUseScope(this) ?: super.getUseScope()
}

class TraitImplementationInfo private constructor(
    val trait: RsTraitItem,
    val traitName: String,
    traitMembers: RsMembers,
    implMembers: RsMembers
) {
    val declared = traitMembers.abstractable().filter { it.existsAfterExpansionSelf }
    private val implemented = implMembers.abstractable()
    private val declaredByName = declared.associateBy { it.name!! }
    private val implementedByNameAndType = implemented.associateBy { it.name!! to it.elementType }

    val missingImplementations: List<RsAbstractable> =
        declared.filter { it.isAbstract }.filter { it.name to it.elementType !in implementedByNameAndType }

    val alreadyImplemented: List<RsAbstractable> =
        declared.filter { it.isAbstract }.filter { it.name to it.elementType in implementedByNameAndType }

    val nonExistentInTrait: List<RsAbstractable> = implemented.filter { it.name !in declaredByName }

    val implementationToDeclaration: List<Pair<RsAbstractable, RsAbstractable>> =
        implemented.mapNotNull { imp ->
            val dec = declaredByName[imp.name]
            if (dec != null) imp to dec else null
        }

    private fun RsMembers.abstractable(): List<RsAbstractable> =
        expandedMembers.filter { it.name != null }

    companion object {
        fun create(trait: RsTraitItem, impl: RsImplItem): TraitImplementationInfo? {
            val traitName = trait.name ?: return null
            val traitMembers = trait.members ?: return null
            val implMembers = impl.members ?: return null
            return TraitImplementationInfo(trait, traitName, traitMembers, implMembers)
        }
    }
}

from rust_types import TYPE_TO_REGEX
from rust_types import RustType

# noinspection PyUnresolvedReferences
import lldb_formatters.lldb_providers
from lldb_formatters.lldb_providers import *


def __lldb_init_module(debugger, _dict):
    def register_providers(rust_type, summary=None, synth=None):
        regex = TYPE_TO_REGEX[rust_type].pattern

        if summary:
            func_name = summary.__name__
            debugger.HandleCommand(
                'type summary add -F lldb_formatters.lldb_providers.{} -e -x -h "{}" --category Rust'.format(func_name,
                                                                                                             regex)
            )
        if synth:
            class_name = synth.__name__
            debugger.HandleCommand(
                'type synthetic add -l lldb_formatters.lldb_providers.{} -x "{}" --category Rust'.format(class_name,
                                                                                                         regex)
            )

    register_providers(RustType.STRING, StdStringSummaryProvider)
    register_providers(RustType.STR, StdStrSummaryProvider)
    register_providers(RustType.SLICE, SizeSummaryProvider, StdSliceSyntheticProvider)
    register_providers(RustType.MSVC_SLICE, SizeSummaryProvider, StdSliceSyntheticProvider)
    register_providers(RustType.OS_STRING, StdOsStringSummaryProvider)
    register_providers(RustType.OS_STR, StdOsStrPathSummaryProvider)
    register_providers(RustType.PATH_BUF, StdPathBufSummaryProvider)
    register_providers(RustType.PATH, StdOsStrPathSummaryProvider)

    register_providers(RustType.CSTRING, StdCStringSummaryProvider)
    register_providers(RustType.CSTR, StdCStringSummaryProvider)
    register_providers(RustType.VEC, SizeSummaryProvider, StdVecSyntheticProvider)
    register_providers(RustType.VEC_DEQUE, SizeSummaryProvider, StdVecDequeSyntheticProvider)
    register_providers(RustType.HASH_MAP, SizeSummaryProvider, StdHashMapSyntheticProvider)
    register_providers(RustType.HASH_SET, SizeSummaryProvider, StdHashSetSyntheticProvider)

    register_providers(RustType.RC, StdRcSummaryProvider, StdRcSyntheticProvider)
    register_providers(RustType.RC_WEAK, StdRcSummaryProvider, StdRcSyntheticProvider)
    register_providers(RustType.ARC, StdRcSummaryProvider, StdArcSyntheticProvider)
    register_providers(RustType.ARC_WEAK, StdRcSummaryProvider, StdArcSyntheticProvider)

    register_providers(RustType.CELL, None, StdCellSyntheticProvider)
    register_providers(RustType.REF, StdRefSummaryProvider, StdRefSyntheticProvider)
    register_providers(RustType.REF_MUT, StdRefSummaryProvider, StdRefSyntheticProvider)
    register_providers(RustType.REF_CELL, StdRefSummaryProvider, StdRefCellSyntheticProvider)
    register_providers(RustType.NONZERO_NUMBER, StdNonZeroNumberSummaryProvider)
    register_providers(RustType.RANGE, StdRangeSummaryProvider)
    register_providers(RustType.RANGE_FROM, StdRangeFromSummaryProvider)
    register_providers(RustType.RANGE_INCLUSIVE, StdRangeInclusiveSummaryProvider)
    register_providers(RustType.RANGE_TO, StdRangeToSummaryProvider)
    register_providers(RustType.RANGE_TO_INCLUSIVE, StdRangeToInclusiveSummaryProvider)

    debugger.HandleCommand('type category enable Rust')

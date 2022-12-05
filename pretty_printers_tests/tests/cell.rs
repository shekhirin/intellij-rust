// === LLDB TESTS ==================================================================================

// lldb-command:run

// lldb-command:print cell
// lldb-unix-check:(core::cell::Cell<i32>) cell = { value = 42 }
// lldb-windows-check:(core::cell::Cell<i32>) $0 = { value = 42 }

// lldb-command:print ref_cell1
// lldb-unix-check:(core::cell::RefCell<i32>) ref_cell1 = borrow=1 { value = 42 }
// lldb-windows-check:(core::cell::RefCell<i32>) $1 = borrow=1 { value = 42 }

// lldb-command:print ref1
// lldb-unix-check:(core::cell::Ref<i32>) ref1 = borrow=1 { [...] = 42 }
// lldb-windows-check:(core::cell::Ref<i32>) $2 = borrow=1 { [...] = 42 }

// lldb-command:print ref_mut2
// lldb-unix-check:(core::cell::RefMut<i32>) ref_mut2 = borrow_mut=1 { [...] = 42 }
// lldb-windows-check:(core::cell::RefMut<i32>) $3 = borrow_mut=1 { [...] = 42 }

// === GDB TESTS ==================================================================================

// gdb-command:run

// gdb-command:print cell
// gdb-check:[...]$1 = {value = 42}
// gdb-command:print ref_cell1
// gdb-check:[...]$2 = borrow=1 = {value = 42, borrow = 1}
// gdb-command:print ref1
// gdb-check:[...]$3 = borrow=1 = {[...] = 42, borrow = 1}
// gdb-command:print ref_mut2
// gdb-check:[...]$4 = borrow_mut=1 = {[...] = 42, borrow = -1}


use std::cell::{Cell, RefCell};

fn main() {
    let cell = Cell::new(42);

    let ref_cell1 = RefCell::new(42);
    let ref1 = ref_cell1.borrow();

    let ref_cell2 = RefCell::new(42);
    let ref_mut2 = ref_cell2.borrow_mut();

    print!(""); // #break
}

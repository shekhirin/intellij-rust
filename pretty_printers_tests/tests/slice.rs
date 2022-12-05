// === LLDB TESTS ==================================================================================

// lldb-command:run

// lldb-command:print slice
// lldb-unix-check:(&[i32]) slice = size=2 { [0] = 1 [1] = 2 }
// lldb-windows-check:(slice$<i32>) $0 = size=2 { [0] = 1 [1] = 2 }

// lldb-command:print slice_mut
// lldb-unix-check:(&mut [i32]) slice_mut = size=2 { [0] = 1 [1] = 2 }
// lldb-windows-check:(slice$<i32>) $1 = size=2 { [0] = 1 [1] = 2 }

// === GDB TESTS ===================================================================================

// gdb-command:run

// gdb-command:print slice
// gdb-check:[...]$1 = size=2 = {1, 2}
// gdb-command:print slice_mut
// gdb-check:[...]$2 = size=2 = {1, 2}

fn main() {
    let slice: &[i32] = &[1, 2];
    let slice_mut: &mut [i32] = &mut [1, 2];

    print!(""); // #break
}

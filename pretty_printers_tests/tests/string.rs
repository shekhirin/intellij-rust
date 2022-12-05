// === LLDB TESTS ==================================================================================

// lldb-command:run

// lldb-command:print s1
// lldb-unix-check:(&str) s1 = "A∆й中" [...]
// lldb-windows-stable-check:(str) $0 = "A∆й中" [...]
// lldb-windows-nightly-check:(ref$<str$>) $0 = "A∆й中" [...]

// lldb-command:print s2
// lldb-unix-check:(alloc::string::String) s2 = "A∆й中" [...]
// lldb-windows-check:(alloc::string::String) $1 = "A∆й中" [...]

// lldb-command:print s3
// lldb-unix-check:(&mut str) s3 = "A∆й中" [...]
// lldb-windows-stable-check:(str) $2 = "A∆й中" [...]
// lldb-windows-nightly-check:(ref_mut$<str$>) $2 = "A∆й中" [...]

// lldb-command:print s4
// lldb-unix-check:(*mut str) s4 = "A∆й中" [...]
// TODO: update pretty-printer (does not work since Rust 1.55) and add `lldb-windows-check`

// lldb-command:print s5
// lldb-unix-check:(*const str) s5 = "A∆й中" [...]
// TODO: update pretty-printer (does not work since Rust 1.55) and add `lldb-windows-check`

// lldb-command:print empty_s1
// lldb-unix-check:(&str) empty_s1 = "" [...]
// lldb-windows-stable-check:(ptr_mut$<str>) $5 = "" [...]
// lldb-windows-nightly-check:(ref$<str$>) $5 = "" [...]

// lldb-command:print empty_s2
// lldb-unix-check:(alloc::string::String) empty_s2 = "" [...]
// lldb-windows-check:(alloc::string::String) $6 = "" [...]

// lldb-command:print empty_s3
// lldb-unix-check:(&mut str) empty_s3 = "" [...]
// lldb-windows-stable-check:(str) $7 = "" [...]
// lldb-windows-nightly-check:(ref_mut$<str$>) $7 = "" [...]

// lldb-command:print empty_s4
// lldb-unix-check:(*mut str) empty_s4 = "" [...]
// TODO: update pretty-printer (does not work since Rust 1.55) and add `lldb-windows-check`

// lldb-command:print empty_s5
// lldb-unix-check:(*const str) empty_s5 = "" [...]
// TODO: update pretty-printer (does not work since Rust 1.55) and add `lldb-windows-check`

// === GDB TESTS ==================================================================================

// gdb-command:run

// gdb-command:print s1
// gdb-check:[...]$1 = "A∆й中"
// gdb-command:print s2
// gdb-check:[...]$2 = "A∆й中"
// gdb-command:print empty_s1
// gdb-check:[...]$3 = ""
// gdb-command:print empty_s2
// gdb-check:[...]$4 = ""


fn main() {
    let mut s1 = "A∆й中";
    let mut s2 = String::from(s1);
    let s3 = s2.as_mut_str();
    let s4 = s3 as *mut str;
    let s5 = s1 as *const str;

    let mut empty_s1 = "";
    let mut empty_s2 = String::from(empty_s1);
    let empty_s3 = empty_s2.as_mut_str();
    let empty_s4 = empty_s3 as *mut str;
    let empty_s5 = empty_s1 as *const str;

    print!(""); // #break
}

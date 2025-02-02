// min-version: 1.60.0

// === LLDB TESTS ==================================================================================

// lldb-command:run

// lldb-command:print os_string
// lldbr-check:[...]os_string = "abc" [...]
// lldbg-check:[...]$0 = "abc" [...]

// lldb-command:print os_string_empty
// lldbr-check:[...]os_string_empty = "" [...]
// lldbg-check:[...]$1 = "" [...]

// lldb-command:print os_string_unicode
// lldbr-check:[...]os_string_unicode = "A∆й中" [...]
// TODO: support WTF-8 on Windows


// lldb-command:print os_str
// lldbr-check:[...]os_str = "abc" { data_ptr = [...] length = 3 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows

// lldb-command:print os_str_empty
// lldbr-check:[...]os_str_empty = "" { data_ptr = [...] length = 0 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows

// lldb-command:print os_str_unicode
// lldbr-check:[...]os_str_unicode = "A∆й中" { data_ptr = [...] length = 9 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings and WTF-8 on Windows


// lldb-command:print c_string
// lldbr-check:[...]c_string = "abc" [...]
// MSVC LLDB without Rust support fails to print `CString` in the console (but CLion renders it fine)

// lldb-command:print c_string_empty
// lldbr-check:[...]c_string_empty = "" [...]
// MSVC LLDB without Rust support fails to print `CString` in the console (but CLion renders it fine)

// lldb-command:print c_string_unicode
// lldbr-check:[...]c_string_unicode = "A∆й中" [...]
// MSVC LLDB without Rust support fails to print `CString` in the console (but CLion renders it fine)


// lldb-command:print c_str
// lldbr-check:[...]c_str = "abcd" { data_ptr = [...] length = 5 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows

// lldb-command:print c_str_empty
// lldbr-check:[...]c_str_empty = "" { data_ptr = [...] length = 1 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows

// lldb-command:print c_str_unicode
// lldbr-check:[...]c_str_unicode = "A∆й中" { data_ptr = [...] length = 10 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows


// lldb-command:print path_buf
// lldbr-check:[...]path_buf = "/a/b/c" [...]
// lldbg-check:[...]$12 = "/a/b/c" [...]

// lldb-command:print path_buf_empty
// lldbr-check:[...]path_buf_empty = "" [...]
// lldbg-check:[...]$13 = "" [...]

// lldb-command:print path_buf_unicode
// lldbr-check:[...]path_buf_unicode = "/a/b/∂" [...]
// TODO: support WTF-8 on Windows


// lldb-command:print path
// lldbr-check:[...]path = "/a/b/c" { data_ptr = [...] length = 6 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows

// lldb-command:print path_empty
// lldbr-check:[...]path_empty = "" { data_ptr = [...] length = 0 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows

// lldb-command:print path_unicode
// lldbr-check:[...]path_unicode = "/a/b/∂" { data_ptr = [...] length = 8 }
// TODO: support `ref$<...>` and `ref_mut$<...>` type wrappings on Windows


// === GDB TESTS ==================================================================================

// gdb-command:run

// gdb-command:print os_string
// gdb-check:[...]$1 = "abc"
// gdb-command:print os_string_empty
// gdb-check:[...]$2 = ""
// gdb-command:print os_string_unicode
// gdb-check:[...]$3 = "A∆й中"


use std::ffi::{CStr, CString, OsStr, OsString};
use std::ops::DerefMut;
use std::path::{Path, PathBuf};

fn main() {
    let mut os_string = OsString::from("abc");
    let os_string_empty = OsString::from("");
    let mut os_string_unicode = OsString::from("A∆й中");

    let os_str = os_string.deref_mut();
    let os_str_empty = OsStr::new("");
    let os_str_unicode = os_string_unicode.deref_mut();

    let c_string = CString::new("abc").unwrap();
    let c_string_empty = CString::new("").unwrap();
    let c_string_unicode = CString::new("A∆й中").unwrap();

    let c_str = CStr::from_bytes_with_nul(b"abcd\0").unwrap();
    let c_str_empty = CStr::from_bytes_with_nul(b"\0").unwrap();
    let c_str_unicode = CStr::from_bytes_with_nul(b"A\xE2\x88\x86\xD0\xB9\xE4\xB8\xAD\0").unwrap();

    let path_buf = PathBuf::from("/a/b/c");
    let path_buf_empty = PathBuf::from("");
    let path_buf_unicode = PathBuf::from("/a/b/∂");

    let path = Path::new("/a/b/c");
    let path_empty = Path::new("");
    let path_unicode = Path::new("/a/b/∂");

    print!(""); // #break
}

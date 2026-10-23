use kiebord_engine::dictionary::compile_combined_file;
use std::fs;
use std::path::Path;

fn main() {
    let args: Vec<String> = std::env::args().collect();

    // 1. If explicit input and output are provided
    if args.len() >= 3 {
        let in_path = Path::new(&args[1]);
        let out_path = Path::new(&args[2]);

        if in_path.is_file() {
            // Single file compilation: compile_dict <input.combined> <output.dict>
            compile_combined_file(in_path, out_path).expect("Failed to compile combined file");
            println!("Successfully compiled {:?} -> {:?}", in_path, out_path);
            return;
        } else if in_path.is_dir() {
            // Directory compilation: compile_dict <src_dir> <out_dir>
            fs::create_dir_all(out_path).expect("Failed to create output directory");
            let mut count = 0;
            for entry in fs::read_dir(in_path).expect("Failed to read source directory").flatten() {
                let path = entry.path();
                if path.extension().and_then(|e| e.to_str()) == Some("combined") {
                    let stem = path.file_stem().unwrap();
                    let target = out_path.join(format!("{}.dict", stem.to_string_lossy()));
                    compile_combined_file(&path, &target).expect("Failed to compile combined file");
                    println!("Compiled {:?} -> {:?}", path.file_name().unwrap(), target);
                    count += 1;
                }
            }
            println!("Successfully compiled {} dictionary files to {:?}", count, out_path);
            return;
        }
    }

    // 2. Default workspace compilation: assets-src/dictionaries -> feature/ime/src/main/assets/dictionaries
    let src_dir = Path::new("../../assets-src/dictionaries");
    let out_dir = Path::new("../../feature/ime/src/main/assets/dictionaries");
    fs::create_dir_all(out_dir).expect("Failed to create out dir");

    let mut count = 0;
    if let Ok(entries) = fs::read_dir(src_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.extension().and_then(|e| e.to_str()) == Some("combined") {
                let stem = path.file_stem().unwrap();
                let target = out_dir.join(format!("{}.dict", stem.to_string_lossy()));
                compile_combined_file(&path, &target).expect("Failed to compile combined file");
                println!("Compiled {:?} -> {:?}", path.file_name().unwrap(), target);
                count += 1;
            }
        }
    }

    if count == 0 {
        eprintln!("Usage:");
        eprintln!("  cargo run --bin compile_dict <input.combined> <output.dict>");
        eprintln!("  cargo run --bin compile_dict <src_dir> <out_dir>");
        eprintln!("  cargo run --bin compile_dict (compiles assets-src/dictionaries/*.combined)");
    } else {
        println!("Successfully compiled {} dictionary files.", count);
    }
}

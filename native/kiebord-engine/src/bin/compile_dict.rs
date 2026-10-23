use kiebord_engine::dictionary::generate_dict_assets;
use std::path::Path;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let combined_dir = if args.len() > 1 {
        Path::new(&args[1])
    } else {
        Path::new("../../assets-src/dictionaries")
    };
    let out_dir = if args.len() > 2 {
        Path::new(&args[2])
    } else {
        Path::new("../../feature/ime/src/main/assets/dictionaries")
    };

    generate_dict_assets(combined_dir, out_dir).expect("Failed to compile binary .dict files from .combined");
    println!("Successfully compiled en.dict and bn.dict from {:?} to {:?}", combined_dir, out_dir);
}

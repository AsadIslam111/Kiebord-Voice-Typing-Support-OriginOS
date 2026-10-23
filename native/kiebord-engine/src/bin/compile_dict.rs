use kiebord_engine::dictionary::generate_dict_assets;
use std::path::Path;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let out_dir = if args.len() > 1 {
        Path::new(&args[1])
    } else {
        Path::new("../../feature/ime/src/main/assets/dictionaries")
    };

    generate_dict_assets(out_dir).expect("Failed to compile binary .dict files");
    println!("Successfully compiled en.dict and bn.dict to {:?}", out_dir);
}

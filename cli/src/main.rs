//! NXConvert CLI for Android.
//! Runs nxformat-packer commands from the Android app via ProcessBuilder.

fn main() {
    let args: Vec<String> = std::env::args().collect();
    if let Err(e) = nxformat_packer::cli::run(args, None, None) {
        eprintln!("ERROR: {e}");
        std::process::exit(1);
    }
}

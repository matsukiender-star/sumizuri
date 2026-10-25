import SwiftUI
import Shared

// The app opens the screen written in Kotlin and hands it the links the system opens the app with.
@main
struct SumizuriApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                .onOpenURL { url in
                    IosApp.shared.open(link: url.absoluteString)
                }
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosAppKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

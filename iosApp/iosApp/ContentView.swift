import SwiftUI
import UIKit
import ComposeApp

/// Hosts the shared Compose UI. Edge-to-edge: Compose draws under the status/home bars and applies insets itself.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.all)
    }
}

package sk.ainet.rag.sample.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SKaiNET Local RAG") {
        App()
    }
}

package jr.brian.inindy.presentation.chat

sealed class GroupChatIntent {
    data class DraftChanged(val text: String) : GroupChatIntent()
    data object Send : GroupChatIntent()
    data object LoadOlder : GroupChatIntent()
    data class DeleteMessage(val messageId: String) : GroupChatIntent()
    data object DismissProfanityBlock : GroupChatIntent()
    data object ChatOpened : GroupChatIntent()
}

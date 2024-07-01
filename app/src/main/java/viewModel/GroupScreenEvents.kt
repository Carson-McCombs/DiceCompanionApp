package viewModel

data class GroupScreenEvents (
    val setSelectionMode: (Boolean) -> Unit = {},
    val copySelection: () -> Unit = {},
    val pasteSelection: (Long) -> Unit = {},
    val deleteSelection: () -> Unit = {},
    val navigateTo: (Long) -> Unit = {},
    val navigateUp: () -> Unit = {},
    val navigateToHelpScreen: () -> Unit = {},
)
package viewModel

data class GroupScreenEvents (
    val copy: (Long, Boolean) -> Unit = { _, _ -> },
    val paste: (Long) -> Unit= {},
    val navigateTo: (Long) -> Unit = {},
    val navigateUp: () -> Unit = {},
    val navigateToHelpScreen: () -> Unit = {},
)
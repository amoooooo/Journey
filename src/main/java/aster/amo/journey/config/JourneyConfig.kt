package aster.amo.journey.config

import com.google.gson.annotations.SerializedName

class JourneyConfig(
    var debug: Boolean = false,
    @SerializedName("subtask_separator_character") var subtaskSeparatorCharacter: String = "<gold>┃</gold>",
    @SerializedName("task_description_separator_character") var taskSeparatorCharacter: String = "<gold>┇</gold>",
    @SerializedName("subtask_description_separator") var subtaskDescriptionSeparator: String = "<gold>╏</gold>",
    @SerializedName("quest_sidebar_title") var questSidebarTitle: String = "<gold>Quests",
    @SerializedName("task_description_max_length") var taskDescriptionMaxLength: Int = 48,
    @SerializedName("subtask_description_max_length") var subtaskDescriptionMaxLength: Int = 48,
    @SerializedName("max_tasks_shown") var maxTasksShown: Int = 2,
    @SerializedName("show_description_in_sidebar") var showDescriptionInSidebar: Boolean = false,
) {

    override fun toString(): String {
        return "ExampleModConfig(debug=$debug)"
    }
}

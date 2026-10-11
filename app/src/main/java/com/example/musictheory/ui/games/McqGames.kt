package com.example.musictheory.ui.games
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musictheory.data.MoreContent
import com.example.musictheory.data.QuestionRepo
import com.example.musictheory.ui.common.*
import com.example.musictheory.vm.McqViewModel
import com.example.musictheory.vm.rememberMcqViewModel
@Composable
fun McqGameScaffold(title: String, vm: McqViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    if (ui.finished) { val best by rememberBest(vm.bestKey)
        GameScaffold(title, onBack) {
            ResultPanel("Score: ${ui.score} of ${ui.questions.size * 10}",
                "Best: $best" + if (ui.score >= best && ui.score > 0) " — new record! 🏆" else "") {
                vm.replay() } }
        return }
    val q = ui.questions[ui.index]
    GameScaffold(title, onBack) {
        ScoreRow("Question ${ui.index + 1} of ${ui.questions.size}", "Score: ${ui.score}")
        InfoCard(q.text); Spacer(Modifier.height(14.dp))
        q.options.forEachIndexed { i, opt ->
            val state = when { ui.chosen == null -> OptState.IDLE
                i == q.answer -> OptState.CORRECT; i == ui.chosen -> OptState.WRONG; else -> OptState.MUTED }
            OptionButton(opt, state, enabled = ui.chosen == null) { vm.choose(i) }
            Spacer(Modifier.height(8.dp)) }
        if (ui.chosen != null) {
            Text(q.explanation, fontSize = 14.sp, lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(10.dp))
            Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)) {
                Text(if (ui.index == ui.questions.size - 1) "Finish" else "Next", fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.height(24.dp)) }
}
@Composable fun QuizGameScreen(onBack: () -> Unit) {
    val vm = rememberMcqViewModel("quiz", "best_quiz", "Theory Quiz")
    { QuestionRepo.all.shuffled().take(10) }
    McqGameScaffold("Theory Quiz", vm, onBack) }
@Composable fun MatchGameScreen(onBack: () -> Unit) {
    val vm = rememberMcqViewModel("match", "best_match", "Match the Tradition")
    { MoreContent.allMatchQuestions() }
    McqGameScaffold("Match the Tradition", vm, onBack) }
@Composable fun ModeGameScreen(onBack: () -> Unit) {
    val vm = rememberMcqViewModel("mode", "best_mode", "Name the Mode")
    { QuestionRepo.all.filter { it.topicId == "WESTERN_3" || it.topicId == null }
        .shuffled().take(10) }
    McqGameScaffold("Name the Mode", vm, onBack) }
@Composable fun TopicQuizScreen(topicKey: String, onBack: () -> Unit) {
    val qs = remember2(topicKey) { com.example.musictheory.data.ContentRepo.questionsForTopic(topicKey) }
    if (qs.isEmpty()) { GameScaffold("Topic Quiz", onBack) { ResultPanel("No questions yet", "") {} }
        return }
    val vm = rememberMcqViewModel("topicquiz/$topicKey", "best_topicquiz", "Topic Quiz")
    { qs.shuffled().take(5) }
    McqGameScaffold("Topic Quiz", vm, onBack) }
@Composable fun QuizSetGameScreen(setId: String, onBack: () -> Unit) {
    val vm = rememberMcqViewModel("quizset/$setId", "best_quiz", "Custom Practice")
    { com.example.musictheory.data.QuestionSets.build(setId).shuffled().take(10) }
    McqGameScaffold("Custom Practice", vm, onBack) }
@Composable private fun <T> remember2(key: String, calc: () -> T): T =
    androidx.compose.runtime.remember(key) { calc() }

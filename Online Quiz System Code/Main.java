import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

/**
 * Online Quiz System (console)
 * Concepts: Queue, Stack, Array, Sorting, Methods, File Handling
 *
 * quiz.txt   : question|option A|option B|option C|option D|correct letter
 * scores.txt : name,score,total
 */
public class Main {

    // ---------- small helper classes ----------

    static class Question {
        private final String text;
        private final String[] options;   // 4 options
        private final char answer;        // A, B, C or D

        Question(String text, String[] options, char answer) {
            this.text = text;
            this.options = options;
            this.answer = answer;
        }

        String getText()       { return text; }
        String[] getOptions()  { return options; }
        char getAnswer()       { return answer; }
    }

    static class AnswerRecord {
        private final int questionNo;
        private final char userAnswer;
        private final char correctAnswer;

        AnswerRecord(int questionNo, char userAnswer, char correctAnswer) {
            this.questionNo = questionNo;
            this.userAnswer = userAnswer;
            this.correctAnswer = correctAnswer;
        }

        int getQuestionNo()    { return questionNo; }
        char getUserAnswer()   { return userAnswer; }
        char getCorrectAnswer(){ return correctAnswer; }
    }

    static class ScoreEntry {
        private final String name;
        private final int score;
        private final int total;

        ScoreEntry(String name, int score, int total) {
            this.name = name;
            this.score = score;
            this.total = total;
        }

        String getName() { return name; }
        int getScore()   { return score; }
        int getTotal()   { return total; }
    }

    // ---------- data structures ----------

    static final String QUIZ_FILE = "quiz.txt";
    static final String SCORE_FILE = "scores.txt";
    static final int MAX_SCORES = 100;

    static Queue<Question> questions = new LinkedList<>();          // questions waiting to be asked
    static Stack<AnswerRecord> answered = new Stack<>();            // answered questions (latest on top)
    static ScoreEntry[] scores = new ScoreEntry[MAX_SCORES];        // leaderboard array
    static int scoreCount = 0;

    static Scanner sc = new Scanner(System.in);

    // ---------- file handling ----------

    /** Reads quiz.txt and puts every question in the Queue. */
    static void loadQuestions() {
        questions.clear();
        File f = new File(QUIZ_FILE);
        if (!f.exists()) {
            System.out.println("Warning: " + QUIZ_FILE + " not found.");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|");
                if (p.length != 6) continue;                         // skip a bad line
                char ans = Character.toUpperCase(p[5].trim().charAt(0));
                if (ans < 'A' || ans > 'D') continue;
                String[] opts = { p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim() };
                questions.add(new Question(p[0].trim(), opts, ans));
            }
        } catch (IOException e) {
            System.out.println("Could not read " + QUIZ_FILE + ": " + e.getMessage());
        }
    }

    /** Reads scores.txt and fills the ScoreEntry array. */
    static void loadScores() {
        scoreCount = 0;
        File f = new File(SCORE_FILE);
        if (!f.exists()) return;                                     // first run: no scores yet
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null && scoreCount < MAX_SCORES) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length != 3) continue;
                try {
                    scores[scoreCount++] = new ScoreEntry(p[0].trim(),
                            Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
                } catch (NumberFormatException e) {
                    // ignore a wrong line
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + SCORE_FILE + ": " + e.getMessage());
        }
    }

    /** Writes the score array back to scores.txt. */
    static void saveScores() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SCORE_FILE))) {
            for (int i = 0; i < scoreCount; i++) {
                pw.println(scores[i].getName() + "," + scores[i].getScore() + "," + scores[i].getTotal());
            }
        } catch (IOException e) {
            System.out.println("Could not save " + SCORE_FILE + ": " + e.getMessage());
        }
    }

    // ---------- quiz ----------

    /** Reads one answer. Returns A-D, 'I' if the input is invalid, 'X' if input is finished. */
    static char readAnswer() {
        System.out.print("Your answer (A/B/C/D): ");
        if (!sc.hasNextLine()) return 'X';                           // input finished
        String s = sc.nextLine().trim().toUpperCase();
        if (s.length() == 1 && s.charAt(0) >= 'A' && s.charAt(0) <= 'D') {
            return s.charAt(0);
        }
        System.out.println("Invalid option. Enter A, B, C or D.");     // not counted
        return 'I';
    }

    static void showQuestion(Question q, int qNo) {
        System.out.println();
        System.out.println("Q" + qNo + ". " + q.getText());
        String[] o = q.getOptions();
        System.out.println("  A) " + o[0]);
        System.out.println("  B) " + o[1]);
        System.out.println("  C) " + o[2]);
        System.out.println("  D) " + o[3]);
    }

    static void startQuiz() {
        System.out.print("Enter player name: ");
        if (!sc.hasNextLine()) return;
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "Player";

        loadQuestions();            // fill the Queue again for this player
        answered.clear();           // clear the Stack
        int total = questions.size();
        if (total == 0) {
            System.out.println("No questions available.");
            return;
        }

        int score = 0;
        int qNo = 0;
        while (!questions.isEmpty()) {
            Question q = questions.poll();                           // remove from the front of the Queue
            qNo++;
            char ans;
            do {
                showQuestion(q, qNo);                                // an invalid answer asks the same question again
                ans = readAnswer();
            } while (ans == 'I');
            if (ans == 'X') break;                                   // no more input

            if (ans == q.getAnswer()) {
                score++;
                System.out.println("Correct!");
            } else {
                System.out.println("Wrong! Correct answer: " + q.getAnswer());
            }
            answered.push(new AnswerRecord(qNo, ans, q.getAnswer())); // latest answer on top
        }

        System.out.println();
        System.out.println("Quiz over. " + name + " scored " + score + "/" + total + ".");

        if (scoreCount < MAX_SCORES) {
            scores[scoreCount++] = new ScoreEntry(name, score, total);
        } else {
            System.out.println("Leaderboard is full, score not stored.");
        }
    }

    // ---------- review ----------

    static void reviewAnswers() {
        if (answered.isEmpty()) {
            System.out.println("Nothing to review. Play a quiz first.");
            return;
        }
        Stack<AnswerRecord> temp = new Stack<>();
        while (!answered.isEmpty()) {                                // latest first
            AnswerRecord r = answered.pop();
            System.out.println("Q" + r.getQuestionNo() + " | Your answer: " + r.getUserAnswer()
                    + " | Correct: " + r.getCorrectAnswer());
            temp.push(r);
        }
        while (!temp.isEmpty()) {                                    // put them back
            answered.push(temp.pop());
        }
    }

    // ---------- leaderboard ----------

    /** Bubble sort: highest score first (equal scores keep their old order). */
    static void sortScores() {
        for (int i = 0; i < scoreCount - 1; i++) {
            for (int j = 0; j < scoreCount - 1 - i; j++) {
                if (scores[j].getScore() < scores[j + 1].getScore()) {
                    ScoreEntry t = scores[j];
                    scores[j] = scores[j + 1];
                    scores[j + 1] = t;
                }
            }
        }
    }

    static void showLeaderboard() {
        if (scoreCount == 0) {
            System.out.println("No scores yet.");
            return;
        }
        sortScores();
        System.out.println("----- Leaderboard -----");
        for (int i = 0; i < scoreCount; i++) {
            System.out.println((i + 1) + ". " + scores[i].getName() + " - "
                    + scores[i].getScore() + "/" + scores[i].getTotal());
        }
    }

    // ---------- main ----------

    static void showMenu() {
        System.out.println();
        System.out.println("===== ONLINE QUIZ SYSTEM =====");
        System.out.println("1. Start quiz");
        System.out.println("2. Review answers");
        System.out.println("3. Leaderboard");
        System.out.println("4. Exit");
        System.out.print("Enter your choice: ");
    }

    public static void main(String[] args) {
        loadQuestions();            // load data at start
        loadScores();

        while (true) {
            showMenu();
            if (!sc.hasNextLine()) break;                            // input finished
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": startQuiz();       break;
                case "2": reviewAnswers();   break;
                case "3": showLeaderboard(); break;
                case "4":
                    saveScores();            // save data on exit
                    System.out.println("Scores saved. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Enter 1, 2, 3 or 4.");
            }
        }
        saveScores();
    }
}

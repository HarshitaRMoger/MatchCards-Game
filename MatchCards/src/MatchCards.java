import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.*;
import javax.swing.*;

public class MatchCards {

    class Card {
        String cardName;
        ImageIcon cardImageIcon;

        Card(String cardName, ImageIcon cardImageIcon) {
            this.cardName = cardName;
            this.cardImageIcon = cardImageIcon;
        }

        public String toString() {
            return cardName;
        }
    }

    String[] animalCardList = {
        "lion", "elephant", "tiger", "rat", "cow",
        "monkey", "rabbit", "panda", "cat", "dog"
    };

    String[] friutCardList = {
        "applu", "avacado", "banana", "blueberry", "cherry",
        "mango", "pineapple", "pomo", "strawberry", "watermelon"
    };

    String[] cartoonCardList = {
        "chutki", "chota", "raju", "jaggu", "kaliya",
        "dolubolu", "ranii", "chin", "maa", "rajj"
    };

    String[] selectedCardList;

    int rows;
    int columns;
    int cardWidth = 90;
    int cardHeight = 128;

    ArrayList<Card> cardSet;
    ImageIcon cardBackImageIcon;

    int boardWidth;
    int boardHeight;

    JFrame frame = new JFrame("Memory Matcher");
    JLabel textLabel = new JLabel();
    JPanel textPanel = new JPanel();
    JPanel boardPanel = new JPanel();
    JPanel restartGamePanel = new JPanel();
    JButton restartButton = new JButton();
    JButton quitButton = new JButton(); // Quit Button

    ArrayList<JButton> board;
    Timer hideCardTimer;
    boolean gameReady = false;
    JButton card1Selected;
    JButton card2Selected;

    int currentPlayer = 1;
    int player1Score = 0;
    int player2Score = 0;
    int player1Errors = 0;
    int player2Errors = 0;

    Clip backgroundMusic;

    MatchCards() {
        // Show splash screen for a few seconds before starting theme selection
        showSplashScreen();
    }

    void showSplashScreen() {
        // Create a splash screen with an image
        JFrame splashFrame = new JFrame();
        splashFrame.setSize(600, 400);
        splashFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        splashFrame.setLocationRelativeTo(null);

        // Display splash screen image
        JLabel splashLabel = new JLabel(new ImageIcon(getClass().getResource("./img/memory.png")));
        splashFrame.getContentPane().add(splashLabel);
        splashFrame.setUndecorated(true);
        splashFrame.setVisible(true);

        // After 3 seconds, close splash screen and call theme selection
        Timer splashTimer = new Timer(3000, e -> {
            splashFrame.dispose(); // Close splash screen
            showThemeSelection();
        });
        splashTimer.setRepeats(false);
        splashTimer.start();
    }

    void showThemeSelection() {
        String theme = selectTheme();
        
        // Set the correct card list based on the selected theme
        if (theme.equals("Animals")) {
            selectedCardList = animalCardList;
        } else if (theme.equals("Fruits")) {
            selectedCardList = friutCardList;
        } else if (theme.equals("Cartoons")) {
            selectedCardList = cartoonCardList;
        }

        int difficulty = selectDifficulty();
        setupGameParameters(difficulty);
        setupMusic();
        setupCards();
        shuffleCards();

        frame.setLayout(new BorderLayout());
        frame.setSize(boardWidth, boardHeight + 100);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Font textFont = new Font("Arial", Font.PLAIN, 16);
        textLabel.setFont(textFont);
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setForeground(new Color(255, 255, 255));
        updateScoreLabel();

        textPanel.setPreferredSize(new Dimension(boardWidth, 70));
        textPanel.setLayout(new BorderLayout());
        textPanel.add(textLabel, BorderLayout.CENTER);
        frame.add(textPanel, BorderLayout.NORTH);

        board = new ArrayList<>();
        boardPanel.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < cardSet.size(); i++) {
            JButton tile = new JButton();
            tile.setPreferredSize(new Dimension(cardWidth, cardHeight));
            tile.setOpaque(true);
            tile.setIcon(cardSet.get(i).cardImageIcon);
            tile.setFocusable(false);
            tile.addActionListener(e -> {
                if (!gameReady) {
                    return;
                }
                JButton tile1 = (JButton) e.getSource();
                if (tile1.getIcon() == cardBackImageIcon) {
                    if (card1Selected == null) {
                        card1Selected = tile1;
                        int index = board.indexOf(card1Selected);
                        card1Selected.setIcon(cardSet.get(index).cardImageIcon);
                    } else if (card2Selected == null) {
                        card2Selected = tile1;
                        int index = board.indexOf(card2Selected);
                        card2Selected.setIcon(cardSet.get(index).cardImageIcon);

                        if (!card1Selected.getIcon().equals(card2Selected.getIcon())) {
                            if (currentPlayer == 1) {
                                player1Errors++;
                            } else {
                                player2Errors++;
                            }
                            hideCardTimer.start();

                            currentPlayer = (currentPlayer == 1) ? 2 : 1;
                            updateScoreLabel();
                        } else {
                            if (currentPlayer == 1) {
                                player1Score++;
                            } else {
                                player2Score++;
                            }
                            updateScoreLabel();
                            card1Selected = null;
                            card2Selected = null;

                            if (player1Score + player2Score == cardSet.size() / 2) {
                                gameReady = false;
                                showWinner();
                            }
                        }
                    }
                }
            });
            board.add(tile);
            boardPanel.add(tile);
        }
        frame.add(boardPanel);

        // Restart Button Configuration
        restartButton.setFont(new Font("Arial", Font.PLAIN, 14));
        restartButton.setText("Restart Game");
        restartButton.setPreferredSize(new Dimension(boardWidth, 30));
        restartButton.setFocusable(false);
        restartButton.setEnabled(false);
        restartButton.setForeground(new Color(255, 255, 255));
        restartButton.setBackground(new Color(0, 102, 204));
        restartButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        restartButton.addActionListener(e -> restartGame());
        
        // Quit Button Configuration
        quitButton.setFont(new Font("Arial", Font.PLAIN, 14));
        quitButton.setText("Quit Game");
        quitButton.setPreferredSize(new Dimension(boardWidth, 30));
        quitButton.setFocusable(false);
        quitButton.setForeground(new Color(255, 255, 255));
        quitButton.setBackground(new Color(204, 0, 0));
        quitButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        quitButton.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to quit?",
                "Quit Game",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (response == JOptionPane.YES_OPTION) {
                System.exit(0); // Exit the game
            }
        });

        // Add both Restart and Quit Buttons to restartGamePanel
        restartGamePanel.setLayout(new GridLayout(2, 1, 5, 5)); // 2 rows and 1 column for buttons
        restartGamePanel.add(restartButton);
        restartGamePanel.add(quitButton);
        frame.add(restartGamePanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);

        hideCardTimer = new Timer(3000, e -> hideCards());
        hideCardTimer.setRepeats(false);
        hideCardTimer.start();
    }

    String selectTheme() {
        String[] options = {"Animals", "Fruits", "Cartoons"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "Select a Theme",
            "Memory Matcher ",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );
        switch (choice) {
            case 0: return "Animals";
            case 1: return "Fruits";
            case 2: return "Cartoons";
            default: return "Animals";
        }
    }

    int selectDifficulty() {
        String[] options = {"Easy (6 cards)", "Medium (12 cards)", "Hard (20 cards)"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "Select Game Level",
            "Memory Matcher",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );
        return choice;
    }

    void setupGameParameters(int difficulty) {
        switch (difficulty) {
            case 0: // Easy
                rows = 2;
                columns = 3;
                break;
            case 1: // Medium
                rows = 3;
                columns = 4;
                break;
            case 2: // Hard
                rows = 4;
                columns = 5;
                break;
            default:
                rows = 2;
                columns = 3;
        }
        boardWidth = columns * cardWidth;
        boardHeight = rows * cardHeight;
    }

    void setupCards() {
        cardSet = new ArrayList<>();
        String[] selectedList = selectedCardList;

        if (selectedList == null) return;

        for (int i = 0; i < rows * columns / 2; i++) {
            String cardName = selectedList[i % selectedList.length];
            Image cardImg = new ImageIcon(getClass().getResource("./img/" + cardName + ".jpg")).getImage();
            ImageIcon cardImageIcon = new ImageIcon(cardImg.getScaledInstance(cardWidth, cardHeight, java.awt.Image.SCALE_SMOOTH));

            Card card = new Card(cardName, cardImageIcon);
            cardSet.add(card);
        }
        cardSet.addAll(cardSet); // Duplicate the cards for matching pairs

        Image cardBackImg = new ImageIcon(getClass().getResource("./img/backkk.jpg")).getImage();
        cardBackImageIcon = new ImageIcon(cardBackImg.getScaledInstance(cardWidth, cardHeight, java.awt.Image.SCALE_SMOOTH));
    }

    void shuffleCards() {
        for (int i = 0; i < cardSet.size(); i++) {
            int j = (int) (Math.random() * cardSet.size());
            Card temp = cardSet.get(i);
            cardSet.set(i, cardSet.get(j));
            cardSet.set(j, temp);
        }
    }

    void hideCards() {
        if (gameReady && card1Selected != null && card2Selected != null) {
            card1Selected.setIcon(cardBackImageIcon);
            card1Selected = null;
            card2Selected.setIcon(cardBackImageIcon);
            card2Selected = null;
        } else {
            for (JButton tile : board) {
                tile.setIcon(cardBackImageIcon);
            }
            gameReady = true;
            restartButton.setEnabled(true);
        }
    }

    
    void updateScoreLabel() {
        // Use HTML to structure the label and dynamically change the background color of the text
        String player1Text = "<span style='background-color: " + (currentPlayer == 1 ? "green" : "transparent") + ";'>Player 1: " 
            + player1Score + " (Errors: " + player1Errors + ")</span>";
        String player2Text = "<span style='background-color: " + (currentPlayer == 2 ? "green" : "transparent") + ";'>Player 2: " 
            + player2Score + " (Errors: " + player2Errors + ")</span>";
        
        textLabel.setText(
            "<html>" +
            player1Text + "<br>" +
            player2Text + "<br>" +
            "<font color='black'>Current Player: " + currentPlayer + "</font>" +
            "</html>"
        );
    
        // Optional: You can set the panel's overall background color to neutral or keep it as is.
        textPanel.setBackground(Color.LIGHT_GRAY);
    }
    

    void showWinner() {
        String winnerMessage;
        if (player1Score > player2Score) {
            winnerMessage = "Player 1 wins!";
        } else if (player2Score > player1Score) {
            winnerMessage = "Player 2 wins!";
        } else {
            winnerMessage = "It's a tie!";
        }
        winnerMessage += "\nTotal Errors: Player 1 - " + player1Errors + ", Player 2 - " + player2Errors;

        JOptionPane.showMessageDialog(frame, winnerMessage, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        int newDifficulty = selectDifficulty();
        setupGameParameters(newDifficulty);
        frame.remove(boardPanel);
        boardPanel = new JPanel(new GridLayout(rows, columns));
        board.clear();
        setupCards();
        shuffleCards();

        for (int i = 0; i < cardSet.size(); i++) {
            JButton tile = new JButton();
            tile.setPreferredSize(new Dimension(cardWidth, cardHeight));
            tile.setOpaque(true);
            tile.setIcon(cardBackImageIcon);
            tile.setFocusable(false);
            tile.addActionListener(e -> {
                if (!gameReady) {
                    return;
                }
                JButton tile1 = (JButton) e.getSource();
                if (tile1.getIcon() == cardBackImageIcon) {
                    if (card1Selected == null) {
                        card1Selected = tile1;
                        int index = board.indexOf(card1Selected);
                        card1Selected.setIcon(cardSet.get(index).cardImageIcon);
                    } else if (card2Selected == null) {
                        card2Selected = tile1;
                        int index = board.indexOf(card2Selected);
                        card2Selected.setIcon(cardSet.get(index).cardImageIcon);

                        if (!card1Selected.getIcon().equals(card2Selected.getIcon())) {
                            if (currentPlayer == 1) {
                                player1Errors++;
                            } else {
                                player2Errors++;
                            }
                            hideCardTimer.start();
                            currentPlayer = (currentPlayer == 1) ? 2 : 1;
                            updateScoreLabel();
                        } else {
                            if (currentPlayer == 1) {
                                player1Score++;
                            } else {
                                player2Score++;
                            }
                            updateScoreLabel();
                            card1Selected = null;
                            card2Selected = null;

                            if (player1Score + player2Score == cardSet.size() / 2) {
                                gameReady = false;
                                showWinner();
                            }
                        }
                    }
                }
            });
            board.add(tile);
            boardPanel.add(tile);
        }
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.pack();
        restartGame();
    }

    void setupMusic() {
        try {
            File musicFile = new File("song.wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading music: " + e.getMessage());
        }
    }

    void restartGame() {
        gameReady = true;
        restartButton.setEnabled(false);
        card1Selected = null;
        card2Selected = null;
        shuffleCards();

        for (int i = 0; i < board.size(); i++) {
            board.get(i).setIcon(cardSet.get(i).cardImageIcon);
        }

        player1Score = 0;
        player2Score = 0;
        player1Errors = 0;
        player2Errors = 0;
        currentPlayer = 1;

        updateScoreLabel();

        Timer displayTimer = new Timer(1000, e -> {
            hideCardTimer.restart();
        });
        displayTimer.setRepeats(false);
        displayTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MatchCards::new);
    }
}

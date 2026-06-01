import javax.swing.*;
import java.awt.*;

public class SciFiMenu {

    public static void main(String[] args) {
        // Exécuter l'interface graphique sur le thread d'événements (EDT)
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        // 1. Configuration de la fenêtre principale
        JFrame frame = new JFrame("Subject 27 - Séquence d'éveil");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 768);
        // NOUVEAU CODE :
// Assurez-vous que l'image "image_984619.jpg" est dans le même dossier que votre code source
BackgroundPanel bgPanel = new BackgroundPanel("Gemini_Generated_Image_nuveesnuveesnuve 2.png");
bgPanel.setLayout(new GridBagLayout()); // Conserve le centrage de l'interface
frame.setContentPane(bgPanel); // Définit l'image comme fond principal de la fenêtre

        // 2. Définition des couleurs et polices
        Color neonCyan = new Color(0, 220, 220); // Cyan style science-fiction
        Color darkCyan = new Color(0, 100, 100);
        
        Font titleFont = new Font("SansSerif", Font.BOLD, 14);
        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 16);
        Font buttonFont = new Font("SansSerif", Font.BOLD, 14);
        Font techFont = new Font("Monospaced", Font.PLAIN, 10);

        // 3. Conteneur principal (colonne verticale)
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setOpaque(false); // Fond transparent pour voir la JFrame

        // --- PANNEAU D'EN-TÊTE (Le grand cadre en haut) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        // Bordure cyan avec un padding interne
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(neonCyan, 2),
                BorderFactory.createEmptyBorder(25, 20, 10, 20)
        ));
        headerPanel.setPreferredSize(new Dimension(650, 160));
        headerPanel.setMaximumSize(new Dimension(650, 160));

        // Textes centraux
        JPanel centerTextPanel = new JPanel();
        centerTextPanel.setLayout(new BoxLayout(centerTextPanel, BoxLayout.Y_AXIS));
        centerTextPanel.setOpaque(false);

        JLabel subjectLabel = new JLabel("SUBJECT: 27");
        subjectLabel.setForeground(neonCyan);
        subjectLabel.setFont(titleFont);
        subjectLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Ajout d'espaces pour simuler l'espacement des lettres (tracking)
        JLabel sequenceLabel = new JLabel("S É Q U E N C E   D ' É V E I L   -   S U J E T   2 7");
        sequenceLabel.setForeground(new Color(220, 220, 220)); // Presque blanc
        sequenceLabel.setFont(subtitleFont);
        sequenceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerTextPanel.add(subjectLabel);
        centerTextPanel.add(Box.createRigidArea(new Dimension(0, 15))); // Espacement vertical
        centerTextPanel.add(sequenceLabel);
        
        // Ligne décorative subtile
        centerTextPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        JSeparator separator = new JSeparator();
        separator.setForeground(darkCyan);
        separator.setBackground(new Color(18, 20, 22));
        separator.setMaximumSize(new Dimension(600, 1));
        centerTextPanel.add(separator);

        headerPanel.add(centerTextPanel, BorderLayout.CENTER);

        // Informations techniques dans les coins inférieurs
        JPanel bottomTechPanel = new JPanel(new BorderLayout());
        bottomTechPanel.setOpaque(false);
        
        JLabel leftInfo = new JLabel("STBL_INIT_0.94");
        leftInfo.setForeground(darkCyan);
        leftInfo.setFont(techFont);
        
        JLabel rightInfo = new JLabel("LOC: SEC_B_WNG_04");
        rightInfo.setForeground(darkCyan);
        rightInfo.setFont(techFont);

        bottomTechPanel.add(leftInfo, BorderLayout.WEST);
        bottomTechPanel.add(rightInfo, BorderLayout.EAST);
        headerPanel.add(bottomTechPanel, BorderLayout.SOUTH);

        // Ajout de l'en-tête au conteneur principal
        mainContainer.add(headerPanel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 25))); // Espace avant les boutons

        // --- BOUTONS DU MENU ---
        mainContainer.add(createMenuButton("JOUER", neonCyan, buttonFont));
        mainContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        mainContainer.add(createMenuButton("COMMANDES", neonCyan, buttonFont));
        mainContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        mainContainer.add(createMenuButton("QUITTER", neonCyan, buttonFont));

        // 4. Ajout final et affichage
        frame.add(mainContainer);
        frame.setLocationRelativeTo(null); // Centre la fenêtre sur l'écran
        frame.setVisible(true);
    }

    // Méthode utilitaire pour générer les boutons avec le bon style
    private static JButton createMenuButton(String text, Color color, Font font) {
        JButton button = new JButton(text);
        button.setForeground(color);
        button.setBackground(new Color(25, 28, 30)); // Couleur de fond du bouton
        button.setFont(font);
        button.setFocusPainted(false); // Retire le carré de focus par défaut
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Bordure fine et padding
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(15, 0, 15, 0) // Hauteur du bouton
        ));

        button.setMaximumSize(new Dimension(650, 50)); // Largeur fixe alignée avec l'en-tête
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Effet visuel au passage de la souris (Hover)
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 40, 40)); // Devient un peu plus clair
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(25, 28, 30)); // Retour à la normale
            }
        });

        return button;
    }
}

class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String filePath) {
        // Chargement de l'image
        backgroundImage = new ImageIcon(filePath).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Dessine l'image pour qu'elle remplisse tout le panneau
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
        }
    }
}
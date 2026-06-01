import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class GameOverAvecFond extends JFrame {

    // Couleurs néon (Cyan)
    private static final Color CYAN_NEON = new Color(0, 255, 255);
    private static final Color CYAN_DARK = new Color(0, 100, 100, 200); // Légère transparence
    
    // Variable pour stocker l'image de fond
    private BufferedImage backgroundImage;

    public GameOverAvecFond() {
        setTitle("Terminal Status - Game Over");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // --- ÉTAPE 1 : Charger l'image de fond ---
        try {
            // Cherche le fichier "screen.jpg" à la racine du projet
            backgroundImage = ImageIO.read(new File("Gemini_Generated_Image_nuveesnuveesnuve 2.png"));
        } catch (IOException e) {
            System.err.println("Erreur : Impossible de charger le fichier 'screen.jpg'.");
            System.err.println("Assurez-vous qu'il est placé à la racine du projet.");
            // En cas d'erreur, on définit un fond noir par défaut pour éviter le plantage
            backgroundImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        }

        // --- ÉTAPE 2 : Créer le panneau principal qui dessine le fond et le cadre ---
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // DESSINER L'IMAGE DE FOND
                if (backgroundImage != null) {
                    // On étire l'image pour qu'elle remplisse toute la fenêtre
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }

                // DESSINER LE CADRE NÉON (Repris de la version précédente)
                int frameW = 600, frameH = 200;
                int x = (getWidth() - frameW) / 2;
                int y = 100;

                // Bordure fine sombre
                g2d.setColor(CYAN_DARK);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRect(x, y, frameW, frameH);
                
                // Coins néon brillants
                g2d.setColor(CYAN_NEON);
                g2d.setStroke(new BasicStroke(2f));
                int cs = 20; // Taille du coin (Corner Size)
                // Haut Gauche
                g2d.draw(new Line2D.Double(x, y, x + cs, y));
                g2d.draw(new Line2D.Double(x, y, x, y + cs));
                // Haut Droite
                g2d.draw(new Line2D.Double(x + frameW, y, x + frameW - cs, y));
                g2d.draw(new Line2D.Double(x + frameW, y, x + frameW, y + cs));
                // Bas Gauche
                g2d.draw(new Line2D.Double(x, y + frameH, x + cs, y + frameH));
                g2d.draw(new Line2D.Double(x, y + frameH, x, y + frameH - cs));
                // Bas Droite
                g2d.draw(new Line2D.Double(x + frameW, y + frameH, x + frameW - cs, y + frameH));
                g2d.draw(new Line2D.Double(x + frameW, y + frameH, x + frameW, y + frameH - cs));
            }
        };
        mainPanel.setLayout(null); // Positionnement absolu pour reproduire l'image

        // --- ÉTAPE 3 : Ajouter les textes et boutons ---

        // Textes à l'intérieur du cadre
        JLabel statusLabel = createLabel("STATUS: TERMINATED", 12, CYAN_NEON);
        statusLabel.setBounds(0, 120, 800, 20);
        mainPanel.add(statusLabel);

        JLabel mainTitle = createLabel("VICTOIRE", 48, CYAN_NEON);
        mainTitle.setFont(new Font("Monospaced", Font.BOLD, 48));
        mainTitle.setBounds(0, 150, 800, 60);
        mainPanel.add(mainTitle);

        // Texte d'ambiance sous le cadre
        JLabel subText = createLabel("Bien joué !!!", 16, Color.WHITE);
        subText.setFont(new Font("Serif", Font.BOLD, 20));
        subText.setBounds(0, 320, 800, 30);
        mainPanel.add(subText);

        // Boutons
        JButton btnRetry = createCyberButton("↻ RECOMMENCER", 380);
        JButton btnQuit = createCyberButton("⏻ QUITTER", 440);

        mainPanel.add(btnRetry);
        mainPanel.add(btnQuit);

        add(mainPanel);
    }

    // Méthode utilitaire pour créer des labels transparents
    private JLabel createLabel(String text, int size, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(color);
        label.setFont(new Font("Monospaced", Font.PLAIN, size));
        label.setOpaque(false); // IMPORTANT : Rendre le fond du label transparent
        return label;
    }

    // Méthode utilitaire pour créer des boutons de style Cyber
    private JButton createCyberButton(String text, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(250, y, 300, 45);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false); // Fond transparent par défaut
        btn.setOpaque(false);
        btn.setForeground(CYAN_NEON);
        btn.setFont(new Font("Monospaced", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createLineBorder(CYAN_DARK, 1));
        
        // Effet visuel au survol de la souris
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBorder(BorderFactory.createLineBorder(CYAN_NEON, 2));
                // Légère couleur de fond au survol (cyan très transparent)
                btn.setBackground(new Color(0, 255, 255, 30));
                btn.setContentAreaFilled(true);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBorder(BorderFactory.createLineBorder(CYAN_DARK, 1));
                btn.setContentAreaFilled(false);
            }
        });
        return btn;
    }

    public static void main(String[] args) {
        // Lancer l'interface dans le thread Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new GameOverAvecFond().setVisible(true));
    }
}
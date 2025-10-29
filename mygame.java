import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;



public class mygame extends JFrame {
    public mygame() {
        setTitle("Slayer Green");
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        showMainMenu();
        setVisible(true);
    }

    private void showMainMenu() {
        getContentPane().removeAll();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK);

        JLabel title = new JLabel("Salayer Green", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton start = new JButton("Start");
        JButton setting = new JButton("Setting");
        JButton exit = new JButton("Exit");
        buttons.add(start);
        buttons.add(setting);
        buttons.add(exit);
        panel.add(buttons, BorderLayout.CENTER);

        add(panel);
        revalidate();
        repaint();

        start.addActionListener(e -> showLevelMenu());
        setting.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Under development", "Settings", JOptionPane.INFORMATION_MESSAGE));
        exit.addActionListener(e -> System.exit(0));
    }

    private void showLevelMenu() {
        getContentPane().removeAll();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.DARK_GRAY);

        JLabel title = new JLabel("Menu", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton forest = new JButton("Forest");
        JButton cave = new JButton("Cave");
        JButton back = new JButton("Back");
        buttons.add(forest);
        buttons.add(cave);
        buttons.add(back);
        panel.add(buttons, BorderLayout.CENTER);

        add(panel);
        revalidate();
        repaint();

        forest.addActionListener(e -> openGame("src/Background/ForestMission1.png", false));
        cave.addActionListener(e -> openGame("src/Background/CaveMission2.png", true));
        back.addActionListener(e -> showMainMenu());
    }

    private void openGame(String bgPath, boolean isCave) {
        getContentPane().removeAll();
        GamePanel gamePanel = new GamePanel(bgPath, isCave);
        add(gamePanel);
        revalidate();
        repaint();
        gamePanel.requestFocusInWindow();
    }

    public void returnToMainMenu() {
        showMainMenu();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new mygame());
    }
}


abstract class Person {
    private int hp;
    private int damage;
    private String status = "normal"; 

    abstract void attack(Person target);
    abstract void die();

    int getHp() { return hp; }
    void setHp(int hp) { this.hp = hp; }
    int getDamage() { return damage; }
    void setDamage(int d) { this.damage = d; }
    String getStatus() { return status; }
    void setStatus(String s) { status = s; }
}


class Hero extends Person {
    private final int MAX = 500;
    public Hero() { setHp(MAX); setDamage(20); }
    int getMaxHp() { return MAX; }

    @Override
    void attack(Person target) {
        target.setHp(target.getHp() - getDamage());
        target.die();
    }
    @Override
    void die() { if (getHp() <= 0) setStatus("died"); }
}


class Goblin extends Person {
    public Goblin() { setHp(200); setDamage(10); }
    @Override
    void attack(Person target) {
        target.setHp(target.getHp() - getDamage());
        target.die();
    }
    @Override
    void die() { if (getHp() <= 0) setStatus("died"); }
}


class GoblinArcher extends Person {
    int x, y;
    boolean faceRight = true;
    Image idle;
    Image[] walk;
    Image[] shoot;
    int walkFrame = 0;
    int shootFrame = 0;
    boolean shooting = false;
    long lastShoot = 0;
    final long COOLDOWN = 5000; // 5 sec
    List<Arrow> arrows = new ArrayList<>();
    long deathTime = 0;
    float alpha = 1f;

    public GoblinArcher(int x, int y) {
        this.x = x; this.y = y;
        setHp(150); setDamage(40);
        idle = load("src/Character/TheArcherGoblin.png");
        walk = new Image[4];
        shoot = new Image[7];
        for (int i=0;i<4;i++) walk[i] = load("src/Character/TheArcherGoblinWalk - " + (i+1) + ".png");
        for (int i=0;i<7;i++) shoot[i] = load("src/Character/TheArcherGoblinShoot - " + (i+1) + ".png");
    }

    private Image load(String p) {
        ImageIcon ic = new ImageIcon(p);
        return ic.getImage();
    }

    void update(long now, int heroX, int heroY) {
        if (getStatus().equals("died")) {
            if (deathTime==0) deathTime = now;
            long elapsed = now - deathTime;
            alpha = Math.max(0, 1f - (float)elapsed/3000f);
            for (Arrow a : arrows) a.update();
            arrows.removeIf(a -> !a.alive);
            return;
        }

        int dx = heroX - x;
        faceRight = heroX > x;
        if (Math.abs(dx) <= 150) {
            walkFrame = 0;
            if (now - lastShoot >= COOLDOWN) {
                shooting = true;
                shootFrame++;
                if (shootFrame == 6) {
                    int arrowX = faceRight ? x + 64 : x - 32;
                    int speed = faceRight ? 6 : -6;
                    arrows.add(new Arrow(arrowX, y + 20, speed, getDamage()));
                }
                if (shootFrame >= 7) {
                    shootFrame = 0;
                    shooting = false;
                    lastShoot = now;
                }
            }
        } else {
            shooting = false;
            if (dx > 5) { x += 2; faceRight = true; }
            else if (dx < -5) { x -= 2; faceRight = false; }
            walkFrame = (walkFrame + 1) % 4;
        }
        for (Arrow a : arrows) a.update();
        arrows.removeIf(a -> !a.alive);
    }

    void draw(Graphics2D g2d) {
        if (getStatus().equals("died")) {
            if (alpha <= 0f) return;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        Image img;
        if (shooting) img = shoot[shootFrame];
        else img = walk[walkFrame];

        if (faceRight) g2d.drawImage(img, x, y, 64, 64, null);
        else g2d.drawImage(img, x + 64, y, -64, 64, null);

        for (Arrow a : arrows) a.draw(g2d);

        if (getStatus().equals("died")) g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    @Override
    void attack(Person target) { target.setHp(target.getHp() - getDamage()); target.die(); }
    @Override
    void die() { setStatus("died"); }
}

class Arrow {
    int x, y;
    int speed;
    int damage;
    boolean alive = true;
    Image img;
    public Arrow(int x, int y, int speed, int damage) {
        this.x = x; this.y = y; this.speed = speed; this.damage = damage;
        img = new ImageIcon("src/Character/Arrow.png").getImage();
    }
    void update() {
        x += speed;
        if (x < -50 || x > 900) alive = false;
    }
    void draw(Graphics2D g2d) {
        g2d.drawImage(img, x, y, 32, 16, null);
    }
}

class DamageText {
    int x, y, value;
    Color color;
    int life = 40;
    DamageText(int x, int y, int v, Color c) { this.x = x; this.y = y; this.value = v; this.color = c; }
    void update() { y -= 1; life--; }
    boolean alive() { return life > 0; }
}

enum GobState { MOVING, ATTACKING, STUNNED, DEAD }
enum HeroState { IDLE, WALK, ATTACK, JUMP }

class GamePanel extends JPanel implements KeyListener, ActionListener {
    private Image bg;
    private boolean isCave;
    private Timer timer;
    private Image heroIdle;
    private Image[] heroWalk = new Image[4];
    private Image[] heroAttack = new Image[4];
    private int heroFrame = 0;
    private int heroX = 50, heroY = 250;
    private boolean heroFaceRight = true;
    private HeroState heroState = HeroState.IDLE;
    private int jumpH = 0;
    private boolean jumping = false;
    private boolean heroDamageApplied = false;
    private Image gobIdle;
    private Image[] gobWalk = new Image[4];
    private Image[] gobAttack = new Image[4];
    private int gobX = 650, gobY = 250;
    private int gobFrame = 0, gobAttackFrame = 0;
    private boolean gobFaceRight = false;
    private GobState gobState = GobState.MOVING;
    private float gobAlpha = 1f;
    private long gobDeathTime = 0;
    private final int gobStun = 2000; // ms
    private long gobStateTime = 0;
    private GoblinArcher archer = null;
    private Hero hero = new Hero();
    private Goblin gob = new Goblin();
    private List<DamageText> dmgTexts = new ArrayList<>();
    private Image fullHeart, halfHeart, voidHeart;
    private String bgPath;
    private boolean bgIsCave;
    public GamePanel(String bgPath, boolean isCave) {
        this.bgPath = bgPath;
        this.bgIsCave = isCave;
        this.isCave = isCave;
        bg = new ImageIcon(bgPath).getImage();
        loadImages();
        if (isCave) archer = new GoblinArcher(400, 250);
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(20, this);
        timer.start();
    }

    private void loadImages() {
        heroIdle = load("src/Character/TheKnight.png");
        for (int i=0;i<4;i++) heroWalk[i] = load("src/Character/TheKnightWalk - " + (i+1) + ".png");
        for (int i=0;i<4;i++) heroAttack[i] = load("src/Character/TheKnightAttack - " + (i+1) + ".png");

        gobIdle = load("src/Character/TheGoblin.png");
        for (int i=0;i<4;i++) gobWalk[i] = load("src/Character/TheGoblinWalk - " + (i+1) + ".png");
        for (int i=0;i<4;i++) gobAttack[i] = load("src/Character/TheGoblinAttack - " + (i+1) + ".png");

        fullHeart = load("src/other/FullHeart.png");
        halfHeart = load("src/other/HalfHeart.png");
        voidHeart = load("src/other/VoidHeart.png");
    }

    private Image load(String p) {
        ImageIcon ic = new ImageIcon(p);
        return ic.getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        if (bg != null) g2.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        g2.setColor(new Color(60, 150, 60));
        if (isCave) g2.setColor(Color.GRAY);
        g2.fillRect(0, 300, getWidth(), getHeight()-300);
        Image heroImg;
        switch (heroState) {
            case WALK: heroImg = heroWalk[heroFrame]; break;
            case ATTACK: heroImg = heroAttack[heroFrame]; break;
            default: heroImg = heroIdle; break;
        }
        if (heroFaceRight) g2.drawImage(heroImg, heroX, heroY - jumpH, 64, 64, null);
        else g2.drawImage(heroImg, heroX + 64, heroY - jumpH, -64, 64, null);
        if (!gob.getStatus().equals("died") || gobAlpha > 0f) {
            if (gob.getStatus().equals("died")) {
                if (gobAlpha <= 0f) {
                } else {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, gobAlpha));
                }
            }
            Image img;
            if (gobState == GobState.MOVING) img = gobWalk[gobFrame];
            else if (gobState == GobState.ATTACKING) img = gobAttack[gobAttackFrame];
            else img = gobIdle;

            if (gobFaceRight) g2.drawImage(img, gobX, gobY, 64, 64, null);
            else g2.drawImage(img, gobX + 64, gobY, -64, 64, null);

            if (gob.getStatus().equals("died")) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        if (archer != null) archer.draw(g2);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        for (DamageText dt : dmgTexts) {
            g2.setColor(dt.color);
            g2.drawString(String.valueOf(dt.value), dt.x, dt.y);
        }
        drawHearts(g2, hero.getHp(), hero.getMaxHp());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        g2.drawString("Hero HP: " + hero.getHp(), 10, 20);
        if (archer != null) g2.drawString("Archer HP: " + (archer.getStatus().equals("died") ? 0 : archer.getHp()), 150, 20);
        g2.drawString("Goblin HP: " + (gob.getStatus().equals("died") ? 0 : gob.getHp()), 300, 20);

        g2.dispose();
    }

    private void drawHearts(Graphics2D g2, int hp, int maxHp) {
        int hearts = 5;
        int per = maxHp / hearts;
        for (int i=0;i<hearts;i++) {
            int hx = 10 + i * 36;
            int rem = hp - i * per;
            if (rem >= per) {
                if (fullHeart != null) g2.drawImage(fullHeart, hx, 30, 32, 32, null);
                else { g2.setColor(Color.RED); g2.fillRect(hx,30,32,32); }
            } else if (rem >= per/2) {
                if (halfHeart != null) g2.drawImage(halfHeart, hx, 30, 32, 32, null);
                else { g2.setColor(Color.RED); g2.fillRect(hx,30,16,32); g2.setColor(Color.GRAY); g2.fillRect(hx+16,30,16,32); }
            } else {
                if (voidHeart != null) g2.drawImage(voidHeart, hx, 30, 32, 32, null);
                else { g2.setColor(Color.DARK_GRAY); g2.fillRect(hx,30,32,32); }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_A) {
            heroX -= 5; heroFaceRight = false; heroState = HeroState.WALK;
        } else if (k == KeyEvent.VK_D) {
            heroX += 5; heroFaceRight = true; heroState = HeroState.WALK;
        } else if (k == KeyEvent.VK_SPACE) {
            if (!jumping) { jumping = true; jumpH = 0; heroState = HeroState.JUMP; }
        } else if (k == KeyEvent.VK_K) {
            if (heroState != HeroState.ATTACK) {
                heroState = HeroState.ATTACK;
                heroFrame = 0;
                heroDamageApplied = false; 
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_A || k == KeyEvent.VK_D) {
            heroState = HeroState.IDLE;
            heroFrame = 0;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {
        if (jumping) {
            jumpH += 12;
            if (jumpH >= 100) jumping = false;
        } else if (jumpH > 0) {
            jumpH -= 12;
            if (jumpH < 0) jumpH = 0;
        }
        if (heroState == HeroState.WALK) heroFrame = (heroFrame + 1) % 4;
        else if (heroState == HeroState.ATTACK) {
            heroFrame++;
            if (heroFrame >= 4) {
                heroFrame = 0;
                heroState = HeroState.IDLE;
            }
        } else heroFrame = 0;

        long now = System.currentTimeMillis();
        if (gob.getStatus().equals("died")) {
            if (gobAlpha > 0f) {
                long elapsed = now - gobDeathTime;
                gobAlpha = Math.max(0f, 1f - (float)elapsed/3000f);
            }
        } else {
            int dx = heroX - gobX;
            if (gobState == GobState.ATTACKING) {
                gobAttackFrame++;
                if (gobAttackFrame == 2) {
                    hero.setHp(hero.getHp() - gob.getDamage());
                    dmgTexts.add(new DamageText(heroX, heroY - jumpH, gob.getDamage(), Color.RED));
                }
                if (gobAttackFrame >= 4) {
                    gobAttackFrame = 0;
                    gobState = GobState.STUNNED;
                    gobStateTime = now;
                }
            } else if (gobState == GobState.STUNNED) {
                if (now - gobStateTime > gobStun) gobState = GobState.MOVING;
            } else { 
                if (dx > 5) { gobFaceRight = true; gobX += 3; }
                else if (dx < -5) { gobFaceRight = false; gobX -= 3; }
                gobFrame = (gobFrame + 1) % 4;
            }
        }
        Rectangle heroRect = new Rectangle(heroX, heroY - jumpH, 64, 64);
        Rectangle gobRect = new Rectangle(gobX, gobY, 64, 64);

        if (heroState == HeroState.ATTACK && heroRect.intersects(gobRect) && !gob.getStatus().equals("died")) {
            if (heroFrame == 2 && !heroDamageApplied) {
                gob.setHp(gob.getHp() - hero.getDamage());
                dmgTexts.add(new DamageText(gobX, gobY, hero.getDamage(), Color.BLACK));
                if (gob.getHp() <= 0) {
                    gob.setStatus("died");
                    gobDeathTime = now;
                } else {
                    gobState = GobState.STUNNED;
                    gobStateTime = now;
                }
                heroDamageApplied = true;
            }
        } else if (gobRect.intersects(heroRect) && gobState == GobState.MOVING && !gob.getStatus().equals("died")) {
            gobState = GobState.ATTACKING;
            gobAttackFrame = 0;
        }
        if (gob.getStatus().equals("died")) {
            if (gobDeathTime == 0) gobDeathTime = now;
            long elapsed = now - gobDeathTime;
            gobAlpha = Math.max(0, 1f - (float)elapsed/3000f);
        }
        if (archer != null) {
            archer.update(now, heroX, heroY);
            Rectangle archerRect = new Rectangle(archer.x, archer.y, 64, 64);
            if (heroState == HeroState.ATTACK && heroRect.intersects(archerRect) && !archer.getStatus().equals("died")) {
                if (heroFrame == 2 && !heroDamageApplied) {
                    archer.setHp(archer.getHp() - hero.getDamage());
                    dmgTexts.add(new DamageText(archer.x, archer.y, hero.getDamage(), Color.BLACK));
                    if (archer.getHp() <= 0) archer.die();
                    heroDamageApplied = true;
                }
            }
            for (Arrow a : new ArrayList<>(archer.arrows)) {
                Rectangle arrowRect = new Rectangle(a.x, a.y, 32, 16);
                if (heroRect.intersects(arrowRect) && a.alive) {
                    hero.setHp(hero.getHp() - a.damage);
                    dmgTexts.add(new DamageText(heroX, heroY - jumpH, a.damage, Color.RED));
                    a.alive = false;
                }
            }
        }
        if (heroState != HeroState.ATTACK) heroDamageApplied = false;
        for (DamageText dt : new ArrayList<>(dmgTexts)) {
            dt.update();
            if (!dt.alive()) dmgTexts.remove(dt);
        }
        checkEndConditions();

        repaint();
    }

    private void checkEndConditions() {
        if (hero.getHp() <= 0) {
            hero.setHp(0);
            timer.stop();
            int sel = JOptionPane.showOptionDialog(
                    this,
                    "You lost!",
                    "Defeat",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new String[]{"Retry", "Main Menu"},
                    "Retry"
            );
            if (sel == 0) restartLevel();
            else goToMainMenu();
        }
        boolean gobDead = gob.getStatus().equals("died");
        boolean archerDead = (archer == null) || archer.getStatus().equals("died") && archer.alpha <= 0f;
        if (gobDead && archerDead) {
            timer.stop();
            int sel = JOptionPane.showOptionDialog(
                    this,
                    "You win!",
                    "Victory",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new String[]{"Main Menu", "Play Again"},
                    "Main Menu"
            );
            if (sel == 0) goToMainMenu();
            else restartLevel();
        }
    }

    private void restartLevel() {
        Container top = SwingUtilities.getWindowAncestor(this);
        if (top instanceof JFrame) {
            JFrame f = (JFrame) top;
            f.getContentPane().removeAll();
            GamePanel gp = new GamePanel(bgPath, bgIsCave);
            f.add(gp);
            f.revalidate();
            f.repaint();
            gp.requestFocusInWindow();
        }
    }

    private void goToMainMenu() {
        Container top = SwingUtilities.getWindowAncestor(this);
        if (top instanceof mygame) {
            mygame mg = (mygame) top;
            mg.returnToMainMenu();
        } else if (top instanceof JFrame) {
            JFrame f = (JFrame) top;
            f.getContentPane().removeAll();
            mygame mg = new mygame();
            f.dispose();
        }
    }
}


package com.votesecure.ui;

import com.votesecure.AppContext;
import com.votesecure.model.Administrator;
import com.votesecure.model.Vote;
import com.votesecure.model.Voter;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Main window and navigation controller for every screen. */
public final class MainFrame extends JFrame {

    public static final String WELCOME = "welcome";
    public static final String VOTER_LOGIN = "voter-login";
    public static final String VOTER_DASHBOARD = "voter-dashboard";
    public static final String VOTE = "vote";
    public static final String VOTE_SUCCESS = "vote-success";
    public static final String RESULTS = "results";
    public static final String ADMIN_LOGIN = "admin-login";
    public static final String ADMIN_DASHBOARD = "admin-dashboard";
    public static final String VOTER_MANAGEMENT = "voter-management";
    public static final String CANDIDATE_MANAGEMENT = "candidate-management";
    public static final String ELECTION_SETTINGS = "election-settings";
    public static final String EXPORT_REPORTS = "export-reports";

    private final AppContext context;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new ScrollableCards(cardLayout);
    private final JScrollPane contentScroll;
    private final JLabel userLabel = new JLabel();
    private final FlatButton signOutButton;
    private final Map<String, Refreshable> refreshables = new HashMap<>();

    private final VoteSuccessPanel voteSuccessPanel;
    private final ResultsPanel resultsPanel;

    private Voter currentVoter;
    private Administrator currentAdmin;

    public MainFrame(AppContext context) {
        super("VoteSecure - Online Voting System");
        this.context = context;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 720));
        setSize(1180, 800);
        setLocationRelativeTo(null);
        setIconImage(createAppIcon());

        cards.setOpaque(false);
        contentScroll = new JScrollPane(cards);
        contentScroll.setBorder(null);
        contentScroll.setOpaque(false);
        contentScroll.getViewport().setBackground(Theme.BACKGROUND);
        contentScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScroll.getVerticalScrollBar().setUnitIncrement(18);

        signOutButton = UiFactory.dangerButton("Sign out");
        signOutButton.setVisible(false);
        signOutButton.addActionListener(event -> signOut());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(contentScroll, BorderLayout.CENTER);
        setContentPane(root);

        voteSuccessPanel = new VoteSuccessPanel(this, context);
        resultsPanel = new ResultsPanel(this, context);

        register(WELCOME, new WelcomePanel(this, context));
        register(VOTER_LOGIN, new VoterLoginPanel(this, context));
        register(ADMIN_LOGIN, new AdminLoginPanel(this, context));
        register(VOTER_DASHBOARD, new VoterDashboardPanel(this, context));
        register(VOTE, new VotePanel(this, context));
        register(VOTE_SUCCESS, voteSuccessPanel);
        register(RESULTS, resultsPanel);
        register(ADMIN_DASHBOARD, new AdminDashboardPanel(this, context));
        register(VOTER_MANAGEMENT, new VoterManagementPanel(this, context));
        register(CANDIDATE_MANAGEMENT, new CandidateManagementPanel(this, context));
        register(ELECTION_SETTINGS, new ElectionSettingsPanel(this, context));
        register(EXPORT_REPORTS, new ExportReportPanel(this, context));

        refreshTopBar();
        showCard(WELCOME);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(12, 24, 12, 24)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        brand.add(new LogoMark(Theme.PRIMARY, Color.WHITE, 38));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new javax.swing.BoxLayout(brandText, javax.swing.BoxLayout.Y_AXIS));
        JLabel name = new JLabel("VoteSecure");
        name.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 19));
        name.setForeground(Theme.TEXT);
        JLabel tagline = new JLabel("Online Voting System");
        tagline.setFont(Theme.SMALL);
        tagline.setForeground(Theme.MUTED);
        brandText.add(name);
        brandText.add(tagline);
        brand.add(brandText);
        bar.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        right.setOpaque(false);
        userLabel.setFont(Theme.SMALL);
        userLabel.setForeground(Theme.MUTED);
        FlatButton homeButton = UiFactory.ghostButton("Home");
        homeButton.addActionListener(event -> goHome());
        right.add(userLabel);
        right.add(homeButton);
        right.add(signOutButton);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private BufferedImage createAppIcon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.PRIMARY);
        g2.fillRoundRect(0, 0, 64, 64, 22, 22);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 36));
        g2.drawString("V", 19, 45);
        g2.dispose();
        return image;
    }

    private void register(String name, JPanel panel) {
        cards.add(panel, name);
        if (panel instanceof Refreshable refreshable) {
            refreshables.put(name, refreshable);
        }
    }

    public void showCard(String name) {
        cardLayout.show(cards, name);
        refreshTopBar();
        SwingUtilities.invokeLater(() -> contentScroll.getViewport().setViewPosition(new Point(0, 0)));
        Refreshable refreshable = refreshables.get(name);
        if (refreshable != null) {
            refreshable.onShow();
        }
    }

    public AppContext getContext() {
        return context;
    }

    public Voter getCurrentVoter() {
        return currentVoter;
    }

    public Administrator getCurrentAdmin() {
        return currentAdmin;
    }

    public void showWelcomeScreen() {
        showCard(WELCOME);
    }

    public void showVoterLoginScreen() {
        showCard(VOTER_LOGIN);
    }

    public void showAdminLoginScreen() {
        showCard(ADMIN_LOGIN);
    }

    public void showVoterDashboard() {
        showCard(VOTER_DASHBOARD);
    }

    public void showVoteScreen() {
        showCard(VOTE);
    }

    public void showAdminDashboard() {
        showCard(ADMIN_DASHBOARD);
    }

    public void showVoterManagement() {
        showCard(VOTER_MANAGEMENT);
    }

    public void showCandidateManagement() {
        showCard(CANDIDATE_MANAGEMENT);
    }

    public void showElectionSettings() {
        showCard(ELECTION_SETTINGS);
    }

    public void showExportReports() {
        showCard(EXPORT_REPORTS);
    }

    public void showResults(boolean administratorLiveView) {
        resultsPanel.setAdministratorView(administratorLiveView);
        showCard(RESULTS);
    }

    public void goBackFromResults() {
        if (currentAdmin != null) {
            showAdminDashboard();
        } else if (currentVoter != null) {
            showVoterDashboard();
        } else {
            showWelcomeScreen();
        }
    }

    public void goHome() {
        if (currentAdmin != null) {
            showAdminDashboard();
        } else if (currentVoter != null) {
            showVoterDashboard();
        } else {
            showWelcomeScreen();
        }
    }

    public void onVoterAuthenticated(Voter voter) {
        currentVoter = voter;
        currentAdmin = null;
        showCard(VOTER_DASHBOARD);
    }

    public void onAdministratorAuthenticated(Administrator administrator) {
        currentAdmin = administrator;
        currentVoter = null;
        showCard(ADMIN_DASHBOARD);
    }

    public void onVoteRecorded(Vote vote) {
        if (currentVoter != null) {
            currentVoter = currentVoter.withHasVoted(true);
        }
        String name = currentVoter == null ? "Voter" : currentVoter.getName();
        voteSuccessPanel.setSubmission(name, vote.getVotedAt());
        showCard(VOTE_SUCCESS);
    }

    public void showVoteSuccess(String voterName, LocalDateTime votedAt) {
        voteSuccessPanel.setSubmission(voterName, votedAt);
        showCard(VOTE_SUCCESS);
    }

    public void signOut() {
        currentVoter = null;
        currentAdmin = null;
        refreshTopBar();
        showCard(WELCOME);
    }

    private void refreshTopBar() {
        if (currentAdmin != null) {
            userLabel.setText("Administrator: " + currentAdmin.getUsername());
        } else if (currentVoter != null) {
            userLabel.setText(currentVoter.getName() + "  |  " + currentVoter.getVoterId());
        } else {
            userLabel.setText("Academic demonstration build");
        }
        signOutButton.setVisible(currentAdmin != null || currentVoter != null);
        signOutButton.setEnabled(currentAdmin != null || currentVoter != null);
    }

    /** Lets each screen use the actual viewport width instead of a wide preferred size. */
    private static final class ScrollableCards extends JPanel implements javax.swing.Scrollable {

        private static final long serialVersionUID = 1L;

        ScrollableCards(CardLayout layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            // CardLayout normally reports the tallest card, which makes short
            // screens scroll and centre themselves in empty space. Size the
            // scrolling area to the screen that is actually visible instead.
            for (java.awt.Component component : getComponents()) {
                if (component.isVisible()) {
                    return component.getPreferredSize();
                }
            }
            return super.getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect,
                                              int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect,
                                               int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}

package com.epam.rd.java.basic.practice8.db;
import com.epam.rd.java.basic.practice8.db.entity.Team;
import com.epam.rd.java.basic.practice8.db.entity.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class DBManager {
    private Connection connection;
    private static DBManager dbManager;
    private static final String SELECT_USERS = "SELECT * FROM users";
    private static final String SELECT_TEAMS = "SELECT * FROM teams";
    private static final String INSERT_USERS = "INSERT INTO users VALUES (?)";
    private static final String INSERT_TEAMS = "INSERT INTO teams VALUES (?)";
    private static final String FIND_USER_BY_LOGIN = "SELECT * FROM users WHERE login=?";
    private static final String FIND_USER_BY_TEAM = "SELECT * FROM users WHERE bame=?";
    private static final String FIND_TEAMS_BY_USER_ID = "SELECT * FROM users WHERE login=?";
    private static final String INSERT_USER_TO_TEAM = "INSERT INTO users_teams VALUES (?, ?)";
    private static final String DELETE_TEAM = "DELETE FROM teams WHERE name=?";
    private static final String UPDATE_TEAM = "UPDATE teams SET name=? WHERE id=?";
    private Logger logger = Logger.getLogger(DBManager.class.getName());

    private DBManager() {
        try {
            connection = getConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static DBManager getInstance() {
        if (dbManager == null) {
            dbManager = new DBManager();
        }
        return dbManager;
    }

    public Connection getConnection() throws SQLException {
        try (FileInputStream input = new FileInputStream("app.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return DriverManager.getConnection(prop.getProperty("connection.url"));
        } catch (IOException ex) {
            logger.severe(ex.getMessage());
        }
        return null;
    }

    public boolean insertUser(User user) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_USERS, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getLogin());
            ps.executeUpdate();
            ResultSet id;
            id = ps.getGeneratedKeys();
            int idFiled = id.getInt(1);
            if (id.next()) {
                user.setId(idFiled);
            }
        } catch (SQLException throwables) {
            System.out.println("insertUser: " + throwables.getSQLState());
            return false;
        }
        return true;
    }

    public boolean insertTeam(Team team) {
        ResultSet id;
        try (PreparedStatement ps = connection.prepareStatement(INSERT_TEAMS, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, team.getName());
            ps.executeUpdate();
            id = ps.getGeneratedKeys();
            int idFiled = id.getInt(1);
            if (id.next()) {
                team.setId(idFiled);
            }
        } catch (SQLException throwables) {
            System.out.println("insertTeam: " + throwables.getSQLState());
            return false;
        }
        return true;
    }

    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        try (Statement ps = connection.createStatement()) {
            ResultSet rs = ps.executeQuery(SELECT_USERS);
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt(1));
                user.setLogin(rs.getString(2));
                users.add(user);
            }
        } catch (SQLException throwables) {
            System.out.println("findAllUsers: " + throwables.getSQLState());
            return Collections.emptyList();
        }
        return users;
    }

    public List<Team> findAllTeams() {
        List<Team> teams = new ArrayList<>();
        try (Statement ps = connection.createStatement()) {
            ResultSet rs = ps.executeQuery(SELECT_TEAMS);
            while (rs.next()) {
                Team team = new Team();
                team.setId(rs.getInt(1));
                team.setName(rs.getString(2));
                teams.add(team);
            }
        } catch (SQLException throwables) {
            System.out.println("findAllTeams: " + throwables.getSQLState());
            return Collections.emptyList();
        }
        return teams;
    }

    public User getUser(String login) {
        User user = null;
        try (PreparedStatement ps = connection.prepareStatement(FIND_USER_BY_LOGIN)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setLogin(rs.getString("login"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return user;
    }

    public Team getTeam(String name) {
        Team team = null;
        try (PreparedStatement ps = connection.prepareStatement(FIND_USER_BY_TEAM)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                team = new Team();
                team.setId(rs.getInt("id"));
                team.setName(rs.getString("name"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return team;
    }

    public List<Team> getUserTeams(User user) {
        List<Team> teams = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(FIND_TEAMS_BY_USER_ID)) {
            ps.setInt(1, user.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Team team = new Team();
                teams.add(team);
                team.setId(rs.getInt(1));
                team.setName(rs.getString(2));
            }
        } catch (SQLException throwables) {
            System.out.println("findAllTeams: " + throwables.getSQLState());
            return Collections.emptyList();
        }
        return teams;
    }

    public boolean setTeamsForUser(User user, Team... team) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_USER_TO_TEAM)){
            connection.setAutoCommit(false);
            for (Team teammate : team) {
                ps.setInt(1, user.getId());
                ps.setInt(2, teammate.getId());
                ps.addBatch();
            }
            int[] usersTeams = ps.executeBatch();
            for (int el : usersTeams) {
                if (el != 1) {
                    return false;
                }
            }
            connection.commit();
            return true;
        } catch (SQLException throwables) {
            System.out.println("setTeamsForUser: " + throwables.getSQLState());
            return false;
        }
    }

    public boolean deleteTeam(Team team) {
        try (PreparedStatement ps = connection.prepareStatement(DELETE_TEAM)) {
            ps.setString(1, team.getName());
            if (ps.executeUpdate() != 1) {
                return false;
            }
        } catch (SQLException throwables) {
            System.out.println("deleteTeam: " + throwables.getSQLState());
            return false;
        }
        return true;
    }

    public boolean updateTeam(Team team) {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_TEAM)) {
            ps.setString(1, team.getName());
            ps.setInt(2, team.getId());
            if (ps.executeUpdate() != 1) {
                return false;
            }
        } catch (SQLException throwables) {
            System.out.println("updateTeam: " + throwables.getSQLState());
            return false;
        }
        return true;
    }
}

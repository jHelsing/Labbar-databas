/* This is the driving engine of the program. It parses the command-line
 * arguments and calls the appropriate methods in the other classes.
 *
 * You should edit this file in two ways:
 * 1) Insert your database username and password in the proper places.
 * 2) Implement the three functions getInformation, registerStudent
 *    and unregisterStudent.
 */

package Task5;

import org.postgresql.util.PSQLException;

import java.sql.*; // JDBC stuff.
import java.util.Properties;
import java.util.Scanner;
import java.io.*;  // Reading user input.

public class StudentPortal
{
    /* Here you should put your database name, username and password */
    static final String USERNAME = "";
    static final String PASSWORD = "";

    /* Print command usage.
     * /!\ you don't need to change this function! */
    public static void usage () {
        System.out.println("Usage:");
        System.out.println("    i[nformation]");
        System.out.println("    r[egister] <course>");
        System.out.println("    u[nregister] <course>");
        System.out.println("    q[uit]");
    }

    /* main: parses the input commands.
     * /!\ You don't need to change this function! */
    public static void main(String[] args) throws Exception
    {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://ate.ita.chalmers.se/";
            Properties props = new Properties();
            props.setProperty("user",USERNAME);
            props.setProperty("password",PASSWORD);
            Connection conn = DriverManager.getConnection(url, props);

            Scanner console = new Scanner(System.in);
            usage();
            System.out.println("Welcome!");
            while(true) {
                System.out.print("? > ");
                String mode = console.nextLine();
                String[] cmd = mode.split(" +");
                cmd[0] = cmd[0].toLowerCase();
                if ("information".startsWith(cmd[0]) && cmd.length == 2) {
                    /* Information mode */
                    getInformation(conn, cmd[1]);
                } else if ("register".startsWith(cmd[0]) && cmd.length == 3) {
                    /* Register student mode */
                    registerStudent(conn, cmd[1], cmd[2]);
                } else if ("unregister".startsWith(cmd[0]) && cmd.length == 3) {
                    /* Unregister student mode */
                    unregisterStudent(conn, cmd[1], cmd[2]);
                } else if ("quit".startsWith(cmd[0])) {
                    break;
                } else usage();
            }
            System.out.println("Goodbye!");
            conn.close();
        } catch (SQLException e) {
            System.err.println(e);
            System.exit(2);
        }
    }

    /* Given a student identification number, this function should print
     * x the name of the student, the students national identification number
     *   and their issued login name (something similar to a CID)
     * x the programme and branch (if any) that the student is following.
     * x the courses that the student has read, along with the grade.
     * x the courses that the student is registered to.
     *      (queue position if the student is waiting for the course)
     * x the number of mandatory courses that the student has yet to read.
     * x whether or not the student fulfills the requirements for graduation
     */
    static void getInformation(Connection conn, String student) throws SQLException  {
        String query = "SELECT * FROM studentsfollowing WHERE studentsfollowing.nationalidnbr = '" + student + "'";
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        System.out.println("Information for student " + student);
        System.out.println("-------------------------------------");

        resultSet.next();
        String studentName = resultSet.getString(2);
        System.out.println("Name: " + studentName);
        String studentID = resultSet.getString(1);
        System.out.println("Student ID: " + studentID);
        System.out.println("Line: " + resultSet.getString(4));
        System.out.println("Branch: " + resultSet.getString(5));

        System.out.println();
        System.out.println("Read courses (name (code), credits: grade)");
        /**
         * evgeny lägg till i kommentaren
         * Gör koden injection safe med ?
         */

        query = "SELECT * FROM finishedcourses WHERE finishedcourses.name = ?";
        resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            String subQuery = "SELECT * FROM course WHERE course.coursename = '" + resultSet.getString(2) + "'";
            Statement s = conn.createStatement();
            ResultSet innerResult = s.executeQuery(subQuery);
            innerResult.next();
            System.out.println("  " + innerResult.getString(2) + "(" + innerResult.getString(1) + "), " + innerResult.getString(3) + "p: " + resultSet.getString(3));
            innerResult.close();
            s.close();
        }

        System.out.println();
        System.out.println("Registered courses (name (code): status)");

        query = "SELECT * FROM registrations WHERE registrations.studentid = '" + studentID + "'";
        resultSet = statement.executeQuery(query);
        while(resultSet.next()) {
            if(resultSet.getString(3).equals("Waiting")) {
                // Student is in the queue for this course
                String innerQuery = "SELECT * FROM coursequeuepositions WHERE coursequeuepositions.studentid = '" + studentID + "' AND coursequeuepositions.coursecode = '" + resultSet.getString(2) + "'";
                Statement s = conn.createStatement();
                ResultSet innerResult = s.executeQuery(innerQuery);
                innerResult.next();

                System.out.println("  " + resultSet.getString(1) + " (" + resultSet.getString(2) + "): Waiting as nr " + innerResult.getString(3));

                innerResult.close();
                s.close();
            } else {
                // Student it registered to this course
                System.out.println("  " + resultSet.getString(1) + " (" + resultSet.getString(2) + "): " + resultSet.getString(3));
            }
        }

        System.out.println();
        query = "SELECT * FROM pathtograduation WHERE pathtograduation.studentid = '" + studentID + "'";
        resultSet = statement.executeQuery(query);
        resultSet.next();

        System.out.println("Seminar courses taken: " + resultSet.getInt(6));
        System.out.println("Math credits taken: " + resultSet.getInt(4));
        System.out.println("Research credits taken: " + resultSet.getInt(5));
        System.out.println("Total credits taken: " + resultSet.getInt(2));
        System.out.println("Fulfills the requirements for graduation: " + resultSet.getString(7));

        System.out.println("-------------------------------------");
        System.out.println();
        System.out.println("Please choose a mode of operation:");
        resultSet.close();
        statement.close();
    }

    /* Register: Given a student id number and a course code, this function
     * should try to register the student for that course.
     */
    static void registerStudent(Connection conn, String student, String course) throws SQLException {
        String query = "SELECT * FROM studentsfollowing WHERE studentsfollowing.nationalidnbr = '" + student + "'";
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        resultSet.next();
        String studentID = resultSet.getString(1);
        resultSet.close();
        query = "INSERT INTO registrations(studentid, coursecode, currentstatus) VALUES ('" + studentID + "', '" + course + "', 'stuff' )";
        try {
            statement.executeUpdate(query);
            System.out.println("Student registered on course");
        } catch (PSQLException e) {
            System.out.println(e.getServerErrorMessage());
        }

        statement.close();
    }

    /* Unregister: Given a student id number and a course code, this function
     * should unregister the student from that course.
     */
    static void unregisterStudent(Connection conn, String student, String course) throws SQLException {
        String query = "SELECT * FROM studentsfollowing WHERE studentsfollowing.nationalidnbr = '" + student + "'";
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        resultSet.next();
        String studentID = resultSet.getString(1);
        resultSet.close();
        query = "DELETE FROM registrations WHERE registrations.studentid = '" + studentID + "' AND registrations.coursecode = '" + course + "'";
        try {
            statement.executeUpdate(query);

            System.out.println("Student no longer registered on course");
        } catch (PSQLException e) {
            System.out.println(e.getServerErrorMessage());
        }
        statement.close();
    }
}

package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

import static ca.ubc.cs317.dict.net.DictStringParser.splitAtoms;
import static ca.ubc.cs317.dict.net.Status.readStatus;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;


    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Status status = readStatus(in);
            if (status.getStatusCode() != 220) {
                throw new DictConnectionException();
            }

        } catch (Exception e) {
           throw new DictConnectionException(e);
        }
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {
        out.println("QUIT");
        try {
            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            // ignore exception
        }
    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();
        out.println("DEFINE " + database.getName() + " " + word);
        Status status = readStatus(in);

        if (status.getStatusCode() == 150) {

            String response = status.getDetails();
            String[] lines = splitAtoms(response);

            int numDefintiions = Integer.parseInt(lines[0]);

            for (int i = 0; i < numDefintiions; i++) {
                status = readStatus(in);
                if (status.getStatusCode() == 151) {
                    response = status.getDetails();
                    lines = splitAtoms(response);

                    Definition definition = new Definition(lines[0], lines[1]);
                    String content;

                    try {
                        content = in.readLine();
                    } catch (Exception e) {
                        throw new DictConnectionException(e);
                    }

                    while (!content.equals(".")) {
                        try {
                            definition.appendDefinition(content);
                            content = in.readLine();
                        } catch (IOException e) {
                            throw new DictConnectionException(e);
                        }
                    }
                    set.add(definition);
                }
            }
            Status finalStatus = readStatus(in);
            if (finalStatus.getStatusCode() != 250) throw new DictConnectionException();
        }
        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();

        out.println("MATCH " + database.getName() + " " + strategy.getName() + " " + word);
        Status status = readStatus(in);

        if (status.getStatusCode() == 152) {
            String line;

            try {
                line = in.readLine();
            } catch (IOException e) {
                throw new DictConnectionException(e);
            }

            while (!line.equals(".")) {

                try {
                    String[] lines = splitAtoms(line);
                    System.out.println(lines[1]);
                    set.add(lines[1]);
                    line = in.readLine();
                } catch (IOException e) {
                    throw new DictConnectionException(e);
                }
            }

            Status finalStatus = readStatus(in);
            if (finalStatus.getStatusCode() != 250) throw new DictConnectionException();
        }

        return set;
    }

    /** Requests and retrieves a map of database name to an equivalent database object for all valid databases used in the server.
     *
     * @return A map of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();

        out.println("SHOW DB");
        Status status = readStatus(in);

        if (status.getStatusCode() == 110) {
            try {
                String line = in.readLine();
                while (!line.equals(".")) {
                    String[] lines = splitAtoms(line);
                    Database database = new Database(lines[0], lines[1]);
                    databaseMap.put(lines[0], database);
                    line = in.readLine();
                }
            } catch (IOException e) {
                throw new DictConnectionException(e);
            }
            Status finalStatus = readStatus(in);
            if (finalStatus.getStatusCode() != 250) throw new DictConnectionException();
        }
        return databaseMap;
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

        out.println("SHOW STRAT");
        Status status = readStatus(in);
        String line;

        try {
            if (status.getStatusCode() == 111) {
                line = in.readLine();
                while (!line.equals(".")) {
                    String[] lines = splitAtoms(line);
                    MatchingStrategy ms = new MatchingStrategy(lines[0], lines[1]);
                    set.add(ms);
                    line = in.readLine();
                }

                Status finalStatus = readStatus(in);
                if (finalStatus.getStatusCode() != 250) throw new DictConnectionException();
            }
        } catch (IOException e) {
            throw new DictConnectionException(e);
        }

        return set;
    }

    /** Requests and retrieves detailed information about the currently selected database.
     *
     * @return A string containing the information returned by the server in response to a "SHOW INFO <db>" command.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized String getDatabaseInfo(Database d) throws DictConnectionException {
	StringBuilder sb = new StringBuilder();

        out.println("SHOW INFO" + d.getName());
        Status status = readStatus(in);
        String line;

        try {
            if (status.getStatusCode() == 112) {
                line = in.readLine();
                while (!line.equals(".")) {
                    sb.append(line);
                    line = in.readLine();
                }
            }
            if (status.getStatusCode() == 550) {
                out.println("invalid database");
            } else {
                throw new DictConnectionException();
            }

        } catch (IOException e) {
            throw new DictConnectionException(e);
        }

        return sb.toString();
    }
}

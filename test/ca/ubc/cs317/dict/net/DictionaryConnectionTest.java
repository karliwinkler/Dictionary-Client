package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DictionaryConnectionTest {
    @Test
    public void testBasicConnection() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        assertNotNull(conn);
    }

    @Test
    public void testGetDatabaseList() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Map<String, Database> dbl = conn.getDatabaseList();
        assertTrue(dbl.size() > 0);
    }

    @Test
    public void testGetDefinition() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Map<String, Database> dbl = conn.getDatabaseList();
        assertTrue(dbl.size() > 0);
        Database wn = dbl.get("wn");
        assertNotNull(wn);
        Collection<Definition> defs = conn.getDefinitions("parrot", wn);
        assertTrue(defs.size() > 0);
    }

    @Test
    public void testGetDefinitionNoMatch() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Map<String, Database> dbl = conn.getDatabaseList();
        assertTrue(dbl.size() > 0);
        Database wn = dbl.get("wn");
        assertNotNull(wn);
        Collection<Definition> defs = conn.getDefinitions("fjsdfnds", wn);
        assertTrue(defs.size() == 0);
    }

    @Test
    public void testGetDatabaseInfo() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Map<String, Database> dbl = conn.getDatabaseList();
        Database wn = dbl.get("wn");
        assertNotNull(wn);
        // String info = conn.getDatabaseInfo(wn);
        // System.out.println(info);
    }

    @Test
    public void testGetMatchingStrategy() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Set<MatchingStrategy> set = conn.getStrategyList();
        assertTrue(set.size() > 0);
        for (MatchingStrategy strategy : set) {
            // System.out.println(strategy);
        }
    }

    @Test
    public void testGetMatchList() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Map<String, Database> dbl = conn.getDatabaseList();
        Database wn = dbl.get("wn");
        assertNotNull(wn);
        Set<MatchingStrategy> msSet = conn.getStrategyList();
        MatchingStrategy ms = msSet.iterator().next();
        assertNotNull(ms);
        Set<String> list = conn.getMatchList("cat", ms, wn);
        assertTrue(list.size() > 0);

        for (String str : list) {
            System.out.println(str);
        }

    }



}

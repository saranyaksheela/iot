package iot.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify JUnit setup is working correctly.
 */
@DisplayName("Basic JUnit Setup Test")
public class BasicJUnitTest {

    @Test
    @DisplayName("Should verify JUnit is working")
    void testBasicAssertion() {
        assertTrue(true, "Basic assertion should pass");
        assertEquals(2, 1 + 1, "Basic math should work");
    }

    @Test 
    @DisplayName("Should handle string operations")
    void testStringOperations() {
        String test = "Hello World";
        assertNotNull(test);
        assertEquals(11, test.length());
        assertTrue(test.contains("World"));
    }
}
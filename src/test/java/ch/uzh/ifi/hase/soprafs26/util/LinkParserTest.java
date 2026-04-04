package ch.uzh.ifi.hase.soprafs26.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkParserTest {

    @Test
    void getLobbyCodeFromUrl_validInput_returnsLobbyCode() {
        // Given
        String url = "/join?code=ABC123";

        // When
        String lobbyCode = LinkParser.getLobbyCodeFromUrl(url);

        // Then
        assertEquals("ABC123", lobbyCode);
    }

    @Test
    void getLobbyCodeFromUrl_multipleParams_returnsLobbyCode() {
        // Given
        String url = "/join?code=ABC123&user=test";

        // When
        String lobbyCode = LinkParser.getLobbyCodeFromUrl(url);

        // Then
        assertEquals("ABC123", lobbyCode);
    }

    @Test
    void getLobbyCodeFromUrl_invalidInput_returnsNull() {
        // Given
        String url = "/join";

        // When
        String lobbyCode = LinkParser.getLobbyCodeFromUrl(url);

        // Then
        assertNull(lobbyCode);
    }
}
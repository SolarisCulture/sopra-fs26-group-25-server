package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	// Player Mappings
    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "team", target = "team")
    @Mapping(source = "role", target = "role")
	@Mapping(source = "host", target = "isHost")
    PlayerDTO convertEntityToPlayerDTO(Player player);

	// Lobby Mappings
	@Mapping(source = "id", target = "id")
    @Mapping(source = "lobbyCode", target = "lobbyCode")
    @Mapping(source = "hostId", target = "hostId")
    @Mapping(source = "playerList", target = "players")
    @Mapping(source = "settings", target = "settings")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "lobbyStatus", target = "lobbyStatus")
    LobbyDTO convertEntityToLobbyDTO(Lobby lobby);
}

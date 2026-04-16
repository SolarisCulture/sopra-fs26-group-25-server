package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class SubscribeDTO {
    private String type;
    private String lobbyCode;
    private Data data;

    public static class Data {
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

}

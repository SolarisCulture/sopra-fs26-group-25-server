package ch.uzh.ifi.hase.soprafs26.constant;

import java.util.List;

public enum Topic {
    SCIENCE("Science", List.of("Physics", "Chemistry", "Math", "Anatomy", "Medicine")),
    HISTORY("History", List.of("Historical figures", "Places", "Events", "Medieval")),
    GEOGRAPHY("Geography", List.of("Countries", "Cities", "Oceans", "Rivers", "Mountains", "Landmarks")),
    SPACE_ASTRONOMY("Space & Astronomy", List.of("Planets", "Stars", "Galaxies", "Space Travel")),
    LANGUAGE("Language", List.of("Foreign languages", "Grammar", "Idioms", "Sayings")),
    LITERATURE("Literature", List.of("Titles", "Authors", "Literary Characters")),
    POLITICS("Politics", List.of("Politicians", "Parties", "Forms of Government")),
    ARCHITECTURE("Architecture", List.of("Buildings", "Architects", "Styles")),
    ART_STAGE("Art & Stage", List.of("Classical music", "Theatre", "Paintings", "Sculptures")),
    TRADITION_BELIEFS("Tradition & Beliefs", List.of("Religion", "Mythology", "Customs", "Legends", "Astrology")),
    FANTASY("Fantasy", List.of("Magic", "Quests", "Mythical Creatures", "World-building")),
    DESIGN("Design", List.of("Fashion", "Furniture", "Interiors", "Designers", "Logos")),
    SOCIETY("Society", List.of("Organisations", "Institutions", "Legislation", "Relationships")),
    FILM("Film", List.of("Actors", "Directors", "Titles", "Film Quotes")),
    DISNEY("Disney", List.of("Characters", "Movies", "Pixar", "Magic", "Heroes", "Villains")),
    TELEVISION("Television", List.of("Programmes", "Series", "Roles", "Hosts")),
    MUSIC("Music", List.of("Artists", "Albums", "Songs", "Lyrics")),
    CELEBRITIES("Celebrities", List.of("Musicians", "Actors", "Sports", "Media Stars")),
    CRIME_MYSTERY("Crime & Mystery", List.of("True Crime", "Detectives", "Noir")),
    NATURE("Nature", List.of("Animals", "Plants", "Geology", "Environment")),
    FOOD_DRINK("Food & Drink", List.of("Gastronomy", "Chefs", "Restaurants", "Cookbooks")),
    HOUSEHOLD("Household", List.of("Appliances", "Tools", "Furniture", "Everyday Items")),
    SPORT("Sport", List.of("Athletes", "Disciplines", "Events", "Records")),
    BUSINESS("Business", List.of("Companies", "Businesspeople", "Products", "Professions")),
    TECHNOLOGY_GAMES("Technology & Games", List.of("IT", "Inventions", "Gaming", "Programming", "Algorithms")),
    STANDARD("Standard", List.of());

    private final String label;
    private final List<String> keywords;

    Topic(String label, List<String> keywords) {
        this.label = label;
        this.keywords = keywords;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getKeywords() {
        return keywords;
    }
}
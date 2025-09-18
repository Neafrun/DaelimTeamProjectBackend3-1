package org.benefitmap.backend.catalog;

public class CatalogItemDto {
    private String id;
    private String title;
    private String description;
    private String agency;

    public CatalogItemDto() {
    }

    public CatalogItemDto(String id, String title, String description, String agency) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.agency = agency;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }
}

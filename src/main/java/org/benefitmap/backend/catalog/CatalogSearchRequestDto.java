package org.benefitmap.backend.catalog;

public class CatalogSearchRequestDto {
    private String age;
    private String region;
    private String incomeLevel;
    private String lifeStage;
    private String familyType;
    private String interest;

    public CatalogSearchRequestDto() {
    }

    public CatalogSearchRequestDto(String age, String region, String incomeLevel,
            String lifeStage, String familyType, String interest) {
        this.age = age;
        this.region = region;
        this.incomeLevel = incomeLevel;
        this.lifeStage = lifeStage;
        this.familyType = familyType;
        this.interest = interest;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getIncomeLevel() {
        return incomeLevel;
    }

    public void setIncomeLevel(String incomeLevel) {
        this.incomeLevel = incomeLevel;
    }

    public String getLifeStage() {
        return lifeStage;
    }

    public void setLifeStage(String lifeStage) {
        this.lifeStage = lifeStage;
    }

    public String getFamilyType() {
        return familyType;
    }

    public void setFamilyType(String familyType) {
        this.familyType = familyType;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }

    @Override
    public String toString() {
        return "CatalogSearchRequestDto{" +
                "age='" + age + '\'' +
                ", region='" + region + '\'' +
                ", incomeLevel='" + incomeLevel + '\'' +
                ", lifeStage='" + lifeStage + '\'' +
                ", familyType='" + familyType + '\'' +
                ", interest='" + interest + '\'' +
                '}';
    }
}
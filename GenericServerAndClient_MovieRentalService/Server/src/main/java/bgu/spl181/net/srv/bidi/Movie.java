package bgu.spl181.net.srv.bidi;

import java.util.LinkedList;
/**
 * This class represents Movie object as provided in Json file
 */
public class Movie {
	
	private String id;
	private String name;
	private String price;
	private LinkedList<String> bannedCountries = null;
	private String availableAmount;
	private String totalAmount;

	public Movie(
			 String id,
			 String name,
			 String price,
			 LinkedList<String> bannedCountries,
			 String availableAmount,
			 String totalAmount) {
		this.id=id;
		this.name=name;
		this.price=price;
		this.bannedCountries=bannedCountries;
		this.availableAmount=availableAmount;
		this.totalAmount=totalAmount;
	}
	/**
	 * This method retrives the id
	 * @return id
	 */
	public Integer getId() {
		return Integer.parseInt(id);
	}
    /**
     * This method sets the id
     * @param id 
     */
	public void setId(Integer id) {
		this.id = String.valueOf(id);
	}
    /**
     * This method gets name
     * @return name
     */
	public String getName() {
	return name;
	}
	/**
     * This method sets name
     */
	public void setName(String name) {
	this.name = name;
	}
	/**
     * This method gets price
     * @return price
     */
	public Integer getPrice() {
		return Integer.parseInt(price);
	}
	/**
     * This method sets price
     */
	public void setPrice(Integer price) {
		this.price = String.valueOf(price);
	}
	/**
     * This method gets list bannedcountries
     * @return countries
     */
	public LinkedList<String> getBannedCountries() {
	return bannedCountries;
	}
	/**
     * This method sets bannedcountries
     */
	public void setBannedCountries(LinkedList<String> bannedCountries) {
	this.bannedCountries = bannedCountries;
	}
	/**
     * This method gets availableamount
     * @return amount
     */
	public Integer getAvailableAmount() {
		return Integer.parseInt(availableAmount);
	}
	/**
     * This method sets amount
     */
	public void setAvailableAmount(Integer availableAmount) {
	this.availableAmount = String.valueOf(availableAmount);
	}
	/**
     * This method gets amount
     * @return Integer
     */
	public Integer getTotalAmount() {
		return Integer.parseInt(totalAmount);
	}
	/**
     * This method sets amount
     */
	public void setTotalAmount(Integer totalAmount) {
		this.availableAmount = String.valueOf(totalAmount);
	}
	
	@Override
	/**
	 * This method is used by request info
	 * Its returns all data of movie
	 * @return string
	 */
	public String toString() {
		String countries="";
		for(String country:bannedCountries)
			countries=countries+" "+"\""+country+"\"";
		return "\""+name+"\" "+availableAmount+" "+price+countries;
	}
	
}

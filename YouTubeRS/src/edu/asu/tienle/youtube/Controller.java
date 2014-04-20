package edu.asu.tienle.youtube;

/*This is a tester class.
 *@author Tien D. Le
 *
 */

public class Controller {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String username="tienttt10";
		VideoRecommendation vr=new VideoRecommendation(username);
		vr.recommend();
		vr.printRecommendedList();
		int index=1;
		vr.openURL(index);
	}

}

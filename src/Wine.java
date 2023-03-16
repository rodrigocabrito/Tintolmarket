

/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

import java.util.ArrayList;

public class Wine {
    private String name = null;
    private String image = null;
    private ArrayList<Integer> stars = new ArrayList<>();

    public Wine(String name, String image, ArrayList<Integer> stars) {
        this.name = name;
        this.image = image;
        this.stars = stars;
    }

    public void classify(int star) {
        if (star > 5) {
            this.stars.add(5);
        } else if (star < 1) {
            this.stars.add(1);
        } else {
            this.stars.add(star);
        }
    }

    public int getAvgRate() {

        if(stars.size() == 0) {
            return 0;
        } else {
            int sum = 0;
            for (Integer i : stars) {
                sum += i;
            }

            return sum/(stars.size());
        }
    }

    public ArrayList<Integer> getStars() {
        return this.stars;
    }

    public String getName() {
        return this.name;
    }

    public String getImage() {
        return this.image;
    }
}

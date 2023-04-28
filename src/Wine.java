/*
 * @authors:
 *      Rodrigo Cabrito 54455
 *      João Costa 54482
 *      João Fraga 44837
 */

import java.util.ArrayList;

public record Wine(String name, String image, ArrayList<Integer> stars) {

    public void classify(int star) {
        if (star > 5) {
            this.stars.add(5);
        } else this.stars.add(Math.max(star, 1));
    }

    public int getAvgRate() {

        if (stars.size() == 0) {
            return 0;
        } else {
            int sum = 0;
            for (Integer i : stars) {
                sum += i;
            }

            return sum / (stars.size());
        }
    }
}

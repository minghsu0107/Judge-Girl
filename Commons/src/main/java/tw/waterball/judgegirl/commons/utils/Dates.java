/*
 *  Copyright 2020 Johnny850807 (Waterball) 潘冠辰
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tw.waterball.judgegirl.commons.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */
public class Dates {
    private final static Calendar NEVER_EXPIRED_IN_LIFETIME_CALENDAR = Calendar.getInstance();
    public final static Date NEVER_EXPIRED_IN_LIFETIME_DATE = NEVER_EXPIRED_IN_LIFETIME_CALENDAR.getTime();

    static {
        NEVER_EXPIRED_IN_LIFETIME_CALENDAR.set(2100, Calendar.AUGUST, 7);
    }

    public static void main(String[] args) {
        System.out.println(NEVER_EXPIRED_IN_LIFETIME_DATE.getTime());
    }
}

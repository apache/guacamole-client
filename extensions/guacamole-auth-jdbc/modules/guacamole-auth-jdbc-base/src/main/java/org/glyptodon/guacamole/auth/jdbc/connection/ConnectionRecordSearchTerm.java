/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.jdbc.connection;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A search term for querying historical connection records. This will contain
 * a the search term in string form and, if that string appears to be a date. a
 * corresponding date range.
 *
 * @author James Muehlner
 */
public class ConnectionRecordSearchTerm {
    
    /**
     * A pattern that can match a year, year and month, or year and month and
     * day.
     */
    private static final Pattern DATE_PATTERN = 
            Pattern.compile("(\\d+)(?:-(\\d+)?(?:-(\\d+)?)?)?");

    /**
     * The index of the group within <code>DATE_PATTERN</code> containing the
     * year number.
     */
    private static final int YEAR_GROUP = 1;

    /**
     * The index of the group within <code>DATE_PATTERN</code> containing the
     * month number, if any.
     */
    private static final int MONTH_GROUP = 2;

    /**
     * The index of the group within <code>DATE_PATTERN</code> containing the
     * day number, if any.
     */
    private static final int DAY_GROUP = 3;

    /**
     * The start of the date range for records that should be retrieved, if the
     * provided search term appears to be a date.
     */
    private final Date startDate;
    
    /**
     * The end of the date range for records that should be retrieved, if the
     * provided search term appears to be a date.
     */
    private final Date endDate;
    
    /**
     * The string that should be searched for.
     */
    private final String term;
    
    /**
     * Parse the given string as an integer, returning the provided default
     * value if the string is null.
     * 
     * @param str
     *     The string to parse as an integer.
     *
     * @param defaultValue
     *     The value to return if <code>str</code> is null.
     * 
     * @return
     *     The parsed value, or the provided default value if <code>str</code>
     *     is null.
     */
    private static int parseInt(String str, int defaultValue) {
        
        if (str == null)
            return defaultValue;
        
        return Integer.parseInt(str);

    }
    
    /**
     * Returns a new calendar representing the last millisecond of the same
     * year as <code>calendar</code>.
     * 
     * @param calendar
     *     The calendar defining the year whose end (last millisecond) is to be
     *     returned.
     *
     * @return
     *     A new calendar representing the last millisecond of the same year as
     *     <code>calendar</code>.
     */
    private static Calendar getEndOfYear(Calendar calendar) {

        // Get first day of next year
        Calendar endOfYear = Calendar.getInstance();
        endOfYear.clear();
        endOfYear.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);

        // Transform into the last millisecond of the given year
        endOfYear.add(Calendar.MILLISECOND, -1);

        return endOfYear;

    }
    
    /**
     * Returns a new calendar representing the last millisecond of the same
     * month and year as <code>calendar</code>.
     * 
     * @param calendar
     *     The calendar defining the month and year whose end (last millisecond) 
     *     is to be returned.
     *
     * @return
     *     A new calendar representing the last millisecond of the same month 
     *     and year as <code>calendar</code>.
     */
    private static Calendar getEndOfMonth(Calendar calendar) {

        // Copy given calender only up to given month
        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.clear();
        endOfMonth.set(Calendar.YEAR,  calendar.get(Calendar.YEAR));
        endOfMonth.set(Calendar.MONTH, calendar.get(Calendar.MONTH));

        // Advance to the last millisecond of the given month
        endOfMonth.add(Calendar.MONTH,        1);
        endOfMonth.add(Calendar.MILLISECOND, -1);

        return endOfMonth;

    }
    
    /**
     * Returns a new calendar representing the last millisecond of the same
     * year, month, and day as <code>calendar</code>.
     * 
     * @param calendar
     *     The calendar defining the year, month, and day whose end 
     *     (last millisecond) is to be returned.
     *
     * @return
     *     A new calendar representing the last millisecond of the same year, 
     *     month, and day as <code>calendar</code>.
     */
    private static Calendar getEndOfDay(Calendar calendar) {

        // Copy given calender only up to given month
        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.clear();
        endOfMonth.set(Calendar.YEAR,         calendar.get(Calendar.YEAR));
        endOfMonth.set(Calendar.MONTH,        calendar.get(Calendar.MONTH));
        endOfMonth.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));

        // Advance to the last millisecond of the given day
        endOfMonth.add(Calendar.DAY_OF_MONTH, 1);
        endOfMonth.add(Calendar.MILLISECOND, -1);

        return endOfMonth;

    }

    /**
     * Creates a new ConnectionRecordSearchTerm representing the given string.
     * If the given string appears to be a date, the start and end dates of the
     * implied date range will be automatically determined and made available
     * via getStartDate() and getEndDate() respectively.
     *
     * @param term
     *     The string that should be searched for.
     */
    public ConnectionRecordSearchTerm(String term) {

        // Search terms absolutely must not be null
        if (term == null)
            throw new NullPointerException("Search terms may not be null");

        this.term = term;

        // Parse start/end of date range if term appears to be a date
        Matcher matcher = DATE_PATTERN.matcher(term);
        if (matcher.matches()) {

            // Retrieve date components from term
            String year  = matcher.group(YEAR_GROUP);
            String month = matcher.group(MONTH_GROUP);
            String day   = matcher.group(DAY_GROUP);
            
            // Parse start date from term
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.clear();
            startCalendar.set(
                Integer.parseInt(year),
                parseInt(month, 0),
                parseInt(day,   1)
            );

            Calendar endCalendar;

            // Derive end date from start date
            if (month == null) {
                endCalendar = getEndOfYear(startCalendar);
            }
            else if (day == null) {
                endCalendar = getEndOfMonth(startCalendar);
            }
            else {
                endCalendar = getEndOfDay(startCalendar);
            }

            // Convert results back into dates
            this.startDate = startCalendar.getTime();
            this.endDate   = endCalendar.getTime();
            
        }

        // The search term doesn't look like a date
        else {
            this.startDate = null;
            this.endDate   = null;
        }

    }

    /**
     * Returns the start of the date range for records that should be retrieved, 
     * if the provided search term appears to be a date.
     * 
     * @return
     *     The start of the date range.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Returns the end of the date range for records that should be retrieved, 
     * if the provided search term appears to be a date.
     * 
     * @return
     *     The end of the date range.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Returns the string that should be searched for.
     * 
     * @return
     *     The search term.
     */
    public String getTerm() {
        return term;
    }

    @Override
    public int hashCode() {
        return term.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null || !(obj instanceof ConnectionRecordSearchTerm))
            return false;

        return ((ConnectionRecordSearchTerm) obj).getTerm().equals(getTerm());

    }

}

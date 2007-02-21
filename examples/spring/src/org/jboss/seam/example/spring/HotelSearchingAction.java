//$Id$
package org.jboss.seam.example.spring;

import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;

@Name("hotelSearch")
@Scope(ScopeType.SESSION)
public class HotelSearchingAction
{
    @In("#{bookingService}")
    private BookingService bookingService;

    private String searchString;
    private int pageSize = 10;
    private int page;

    @DataModel
    private List<Hotel> hotels;

    public void find()
    {       
        page = 0;
        queryHotels();
    }

    public void nextPage()
    {
        page++;
        queryHotels();
    }

    private void queryHotels()
    {
        String searchPattern = searchString==null ? "%" : '%' + searchString.toLowerCase().replace('*', '%') + '%';
        hotels = bookingService.findHotels(searchPattern, page * pageSize, pageSize);      
    }

    public boolean isNextPageAvailable()
    {
        return hotels!=null && hotels.size()==pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getSearchString()
    {
        return searchString;
    }

    public void setSearchString(String searchString)
    {
        this.searchString = searchString;
    }

}

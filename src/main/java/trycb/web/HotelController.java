package trycb.web;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import trycb.model.Result;
import trycb.service.Hotel;

@RestController
@RequestMapping("/api/hotel")
public class HotelController {

    private final Hotel hotelService;


    @Autowired
    public HotelController(Hotel hotelService) {
        this.hotelService = hotelService;
    }

    @RequestMapping(value="/{description}/{location}/", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result findHotelsByDescriptionAndLocation(@PathVariable("location") String location, @PathVariable("description") String desc) {
        return hotelService.findHotels(location, desc);
    }

    @RequestMapping(value="/{description}/", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result findHotelsByDescription(@PathVariable("description") String desc) {
        return hotelService.findHotels(desc);
    }

    @RequestMapping(value="/", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result findAllHotels() {
        return hotelService.findAllHotels();
    }

}

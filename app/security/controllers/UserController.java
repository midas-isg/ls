package security.controllers;

import play.mvc.Controller;
import security.Secured;

import static security.Secured.Authority.ISG_USER;

@Secured(ISG_USER)
public class UserController extends Controller {
}

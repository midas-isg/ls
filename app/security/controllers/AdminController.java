package security.controllers;

import play.mvc.Controller;
import security.Secured;

import static security.Secured.Authority.ISG_ADMIN;

@Secured(ISG_ADMIN)
public class AdminController extends Controller {
}
package StringLiteralDuplicatedCheck

import (
  "net/http"

  "github.com/beego/beego/v2/server/web"
  "github.com/gin-gonic/gin"
  "github.com/go-chi/chi/v5"
  "github.com/gofiber/fiber/v2"
  "github.com/gorilla/mux"
  "github.com/julienschmidt/httprouter"
  "github.com/labstack/echo/v4"
)

func ginRoutes() {
  router := gin.Default()
  router.GET("/users/:id", nil)
  router.POST("/users/:id", nil)
  router.PUT("/users/:id", nil)
  router.DELETE("/users/:id", nil) // Compliant - string literal used as a Gin route path

  router.Handle("GET", "/gin/handle/:id", nil)
  router.Handle("POST", "/gin/handle/:id", nil)
  router.Handle("PUT", "/gin/handle/:id", nil) // Compliant - string literal used as a Gin route path

  group := router.Group("/api/v1/users")
  group.GET("/group/users/:id", nil)
  group.POST("/group/users/:id", nil)
  group.PATCH("/group/users/:id", nil) // Compliant - string literal used as a Gin route group path
}

func ginGroupPrefixes() {
  router := gin.Default()
  router.Group("/api/v1/accounts")
  router.Group("/api/v1/accounts")
  router.Group("/api/v1/accounts") // Compliant - string literal used as a Gin sub-router prefix
}

func netHttpRoutes() {
  http.HandleFunc("/http/users/{id}", nil)
  http.HandleFunc("/http/users/{id}", nil)
  http.Handle("/http/users/{id}", nil) // Compliant - string literal used as a net/http route pattern

  serveMux := http.NewServeMux()
  serveMux.HandleFunc("/mux/users/{id}", nil)
  serveMux.HandleFunc("/mux/users/{id}", nil)
  serveMux.Handle("/mux/users/{id}", nil) // Compliant - string literal used as a http.ServeMux route pattern
}

func echoRoutes() {
  e := echo.New()
  e.GET("/echo/users/:id", nil)
  e.POST("/echo/users/:id", nil)
  e.DELETE("/echo/users/:id", nil) // Compliant - string literal used as an Echo route path

  e.Add("GET", "/echo/add/:id", nil)
  e.Add("POST", "/echo/add/:id", nil)
  e.Add("PUT", "/echo/add/:id", nil) // Compliant - string literal used as an Echo route path

  group := e.Group("/api/v1/orders")
  group.GET("/echo/group/:id", nil)
  group.POST("/echo/group/:id", nil)
  group.HEAD("/echo/group/:id", nil) // Compliant - string literal used as an Echo route group path
}

func echoGroupPrefixes() {
  e := echo.New()
  e.Group("/api/v1/invoices")
  e.Group("/api/v1/invoices")
  e.Group("/api/v1/invoices") // Compliant - string literal used as an Echo sub-router prefix
}

func chiRoutes() {
  r := chi.NewRouter()
  r.Get("/chi/users/{id}", nil)
  r.Post("/chi/users/{id}", nil)
  r.Delete("/chi/users/{id}", nil) // Compliant - string literal used as a Chi route pattern

  r.Method("GET", "/chi/method/{id}", nil)
  r.MethodFunc("POST", "/chi/method/{id}", nil)
  r.MethodFunc("PUT", "/chi/method/{id}", nil) // Compliant - string literal used as a Chi route pattern
}

func chiSubRouters() {
  r := chi.NewRouter()
  r.Route("/api/v1/products", nil)
  r.Mount("/api/v1/products", nil)
  r.Mount("/api/v1/products", nil) // Compliant - string literal used as a Chi sub-router pattern
}

func gorillaMuxRoutes() {
  r := mux.NewRouter()
  r.HandleFunc("/gorilla/users/{id}", nil)
  r.HandleFunc("/gorilla/users/{id}", nil)
  r.Handle("/gorilla/users/{id}", nil) // Compliant - string literal used as a Gorilla Mux route path
}

func gorillaMuxPaths() {
  r := mux.NewRouter()
  r.Methods("GET").Path("/api/v1/sessions")
  r.Methods("POST").Path("/api/v1/sessions")
  r.Path("/api/v1/sessions") // Compliant - string literal used as a Gorilla Mux route path

  r.PathPrefix("/api/v1/webhooks")
  r.PathPrefix("/api/v1/webhooks")
  r.PathPrefix("/api/v1/webhooks") // Compliant - string literal used as a Gorilla Mux path prefix
}

// The route-defining functions of a *mux.Route, the value the builder functions of a *mux.Router return.
func gorillaMuxRoutePaths() {
  r := mux.NewRouter()
  route := r.NewRoute()
  route.Path("/api/v1/tokens")
  route.PathPrefix("/api/v1/tokens")
  route.Path("/api/v1/tokens") // Compliant - string literal used as a route path on a *mux.Route
}

func gorillaMuxRouteChains() {
  r := mux.NewRouter()
  route := r.NewRoute()
  route.Methods("GET").Path("/api/v1/reports")
  route.Methods("POST").Path("/api/v1/reports")
  route.Host("example.com").PathPrefix("/api/v1/reports") // Compliant - string literal used as a route path on a *mux.Route
}

func gorillaMuxRouteSubrouters() {
  // Subrouter() is called on the *mux.Route that PathPrefix returns and gives back a *mux.Router
  sub := mux.NewRouter().PathPrefix("/api/v1/tenants").Subrouter()
  sub.HandleFunc("/gorilla/sub/{id}", nil)
  sub.HandleFunc("/gorilla/sub/{id}", nil)
  sub.Handle("/gorilla/sub/{id}", nil) // Compliant - string literal used as a route path on the *mux.Router of a sub-router
}

func gorillaMuxRouteNonPathArgumentsAreNotExcluded() {
  route := mux.NewRouter().NewRoute()
  // "Name" names a route instead of defining its path, so its argument is still reported
  route.Name("user-detail-route") // Noncompliant {{Define a constant instead of duplicating this literal "user-detail-route" 3 times.}} [[effortToFix=2]]
  //         ^^^^^^^^^^^^^^^^^^^
  route.Name("user-detail-route")
  //        <^^^^^^^^^^^^^^^^^^^
  route.Methods("GET").Name("user-detail-route")
  //                       <^^^^^^^^^^^^^^^^^^^
}

func fiberRoutes() {
  app := fiber.New()
  app.Get("/fiber/users/:id", nil)
  app.Post("/fiber/users/:id", nil)
  app.All("/fiber/users/:id", nil) // Compliant - string literal used as a Fiber route path

  app.Add("GET", "/fiber/add/:id", nil)
  app.Add("POST", "/fiber/add/:id", nil)
  app.Add("PUT", "/fiber/add/:id", nil) // Compliant - string literal used as a Fiber route path
}

func fiberSubRouters() {
  app := fiber.New()
  app.Group("/api/v1/payments")
  app.Route("/api/v1/payments", nil)
  app.Mount("/api/v1/payments", nil) // Compliant - string literal used as a Fiber sub-router prefix
}

func httpRouterRoutes() {
  r := httprouter.New()
  r.GET("/httprouter/users/:id", nil)
  r.POST("/httprouter/users/:id", nil)
  r.DELETE("/httprouter/users/:id", nil) // Compliant - string literal used as a httprouter route path

  r.Handle("GET", "/httprouter/handle/:id", nil)
  r.Handler("POST", "/httprouter/handle/:id", nil)
  r.HandlerFunc("PUT", "/httprouter/handle/:id", nil) // Compliant - string literal used as a httprouter route path
}

func beegoRoutes() {
  web.Get("/beego/users/:id", nil)
  web.Post("/beego/users/:id", nil)
  web.Delete("/beego/users/:id", nil) // Compliant - string literal used as a Beego route pattern

  web.Router("/beego/router/:id", nil)
  web.Router("/beego/router/:id", nil)
  web.Router("/beego/router/:id", nil) // Compliant - string literal used as a Beego route pattern
}

func routePathUsedOutsideRouteDefinition() {
  router := gin.Default()
  router.GET("/reused/:id", nil) // Compliant - string literal used as a Gin route path

  first := "/reused/:id" // Noncompliant {{Define a constant instead of duplicating this literal "/reused/:id" 3 times.}} [[effortToFix=2]]
  //       ^^^^^^^^^^^^^
  second := "/reused/:id"
  //       <^^^^^^^^^^^^^
  third := "/reused/:id"
  //      <^^^^^^^^^^^^^
}

func nonPathArgumentsAreNotExcluded() {
  app := fiber.New()
  // "Static" does not define a route, so its arguments are still reported
  app.Static("/assets", "./public/assets") // Noncompliant {{Define a constant instead of duplicating this literal "./public/assets" 3 times.}} [[effortToFix=2]]
  //                    ^^^^^^^^^^^^^^^^^
  app.Static("/images", "./public/assets")
  //                   <^^^^^^^^^^^^^^^^^
  app.Static("/files", "./public/assets")
  //                  <^^^^^^^^^^^^^^^^^
}

type customRouter struct{}

func (c customRouter) GET(path string, handler any) {}

func routeFunctionOnUnknownTypeIsNotExcluded() {
  var c customRouter
  c.GET("/custom/:id", nil) // Noncompliant {{Define a constant instead of duplicating this literal "/custom/:id" 3 times.}} [[effortToFix=2]]
  //    ^^^^^^^^^^^^^
  c.GET("/custom/:id", nil)
  //   <^^^^^^^^^^^^^
  c.GET("/custom/:id", nil)
  //   <^^^^^^^^^^^^^
}

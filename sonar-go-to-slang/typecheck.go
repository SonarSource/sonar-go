package main

import (
	"embed"
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"io/fs"
	"os"

	"golang.org/x/tools/go/gcexportdata"
)

// PackageExportDataDir is also hardcoded in the go:embed directive below.
const PackageExportDataDir = "packages"

var (
	// This compiler directive embeds the package export data (i.e. the "packages" directory) into the Go executable
	//go:embed packages/*
	packages embed.FS

	packageExportData = map[string]string{
		"crypto":                                        "crypto.o",
		"crypto/aes":                                    "crypto_aes.o",
		"crypto/cipher":                                 "crypto_cipher.o",
		"crypto/des":                                    "crypto_des.o",
		"crypto/dsa":                                    "crypto_dsa.o",
		"crypto/md5":                                    "crypto_md5.o",
		"crypto/rand":                                   "crypto_rand.o",
		"crypto/rc4":                                    "crypto_rc4.o",
		"crypto/tls":                                    "crypto_tls.o",
		"crypto/rsa":                                    "crypto_rsa.o",
		"crypto/sha1":                                   "crypto_sha1.o",
		"crypto/sha256":                                 "crypto_sha256.o",
		"crypto/x509":                                   "crypto_x509.o",
		"crypto/internal/boring":                        "crypto_internal_boring.o",
		"crypto/internal/mlkem768":                      "crypto_internal_mlkem768.o",
		"database/sql":                                  "database_sql.o",
		"github.com/beego/beego/v2/server/web":          "beego_server_web_v2.o",
		"github.com/emersion/go-smtp":                   "emersion_smtp.o",
		"github.com/gin-gonic/gin":                      "gin.o",
		"github.com/gofiber/fiber/v2":                   "fiber_v2.o",
		"github.com/imroc/req/v3":                       "req_v3.o",
		"github.com/jlaffaye/ftp":                       "jlaffaye_ftp.o",
		"github.com/secsy/goftp":                        "secsy_goftp.o",
		"github.com/valyala/fasthttp":                   "fasthttp.o",
		"github.com/xhit/go-simple-mail/v2":             "go_simple_mail.o",
		"gopkg.in/dutchcoders/goftp.v1":                 "dutchcoders_goftp_v1.o",
		"golang.org/x/crypto/argon2":                    "x_crypto_argon2.o",
		"golang.org/x/crypto/bcrypt":                    "x_crypto_bcrypt.o",
		"golang.org/x/crypto/blake2b":                   "x_crypto_blake2b.o",
		"golang.org/x/crypto/blake2s":                   "x_crypto_blake2s.o",
		"golang.org/x/crypto/blowfish":                  "x_crypto_blowfish.o",
		"golang.org/x/crypto/cast5":                     "x_crypto_cast5.o",
		"golang.org/x/crypto/chacha20":                  "x_crypto_chacha20.o",
		"golang.org/x/crypto/chacha20poly1305":          "x_crypto_chacha20poly1305.o",
		"golang.org/x/crypto/md4":                       "x_crypto_md4.o",
		"golang.org/x/crypto/openpgp":                   "x_crypto_openpgp.o",
		"golang.org/x/crypto/openpgp/packet":            "x_crypto_openpgp_packet.o",
		"golang.org/x/crypto/openpgp/s2k":               "x_crypto_openpgp_s2k.o",
		"golang.org/x/crypto/otr":                       "x_crypto_otr.o",
		"golang.org/x/crypto/pbkdf2":                    "x_crypto_pbkdf2.o",
		"golang.org/x/crypto/pkcs12":                    "x_crypto_pkcs12.o",
		"golang.org/x/crypto/pkcs12/internal/rc2":       "x_crypto_pkcs12_internal_rc2.o",
		"golang.org/x/crypto/ripemd160":                 "x_crypto_ripemd160.o",
		"golang.org/x/crypto/scrypt":                    "x_crypto_scrypt.o",
		"golang.org/x/crypto/ssh":                       "x_crypto_ssh.o",
		"golang.org/x/crypto/ssh/internal/bcrypt_pbkdf": "x_crypto_ssh_internal_bcrypt_pbkdf.o",
		"golang.org/x/crypto/tea":                       "x_crypto_tea.o",
		"golang.org/x/crypto/twofish":                   "x_crypto_twofish.o",
		"golang.org/x/crypto/xtea":                      "x_crypto_xtea.o",
		"golang.org/x/net/context/ctxhttp":              "x_net_context_ctxhttp.o",
		"golang.org/x/net/xsrftoken":                    "x_net_xsrftoken.o",
		"golang.org/x/oauth2":                           "x_oauth2.o",
		"golang.org/x/oauth2/jwt/v4":                    "x_oauth2_jwt_v4.o",
		"golang.org/x/sys/unix":                         "x_sys_unix.o",
		"gopkg.in/resty.v1":                             "resty_v1.o",
		"gorm.io/gorm":                                  "gorm.o",
		"gorm.io/gorm/logger":                           "gorm_logger.o",
		"math/rand":                                     "math_rand.o",
		"net":                                           "net.o",
		"net/http":                                      "net_http.o",
		"net/http/httputil":                             "net_http_util.o",
		"net/smtp":                                      "net_smtp.o",
		"net/url":                                       "net_url.o",
		"os":                                            "os.o",
		"os/exec":                                       "os_exec.o",
		"os/user":                                       "os_user.o",
		"path":                                          "path.o",
		"path/filepath":                                 "path_fielpath.o",
		"runtime/debug":                                 "runtime_debug.o",
		"runtime/pprof":                                 "runtime_pprof.o",
	}
)

type localImporter struct{}

func (fi *localImporter) Import(path string) (*types.Package, error) {
	if exportDataFileName, ok := packageExportData[path]; ok {
		return getPackageFromExportData(exportDataFileName, path)
	}
	return getEmptyPackage(path), nil
}

func getPackageFromExportData(exportDataFileName string, path string) (*types.Package, error) {
	file, err := packages.Open(PackageExportDataDir + "/" + exportDataFileName)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while opening file %s: %s\n", exportDataFileName, err)
		return getEmptyPackage(path), nil
	}
	defer func(file fs.File) {
		_ = file.Close()
	}(file)

	imports := make(map[string]*types.Package)
	pkg, err := gcexportdata.Read(file, nil, imports, path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error while reading %s export data: %s\n", path, err)
		return getEmptyPackage(path), nil
	}

	return pkg, nil
}

func getEmptyPackage(path string) *types.Package {
	pkg := types.NewPackage(path, path)
	pkg.MarkComplete()
	return pkg
}

func typeCheckAst(path string, fileSet *token.FileSet, astFile *ast.File, debugTypeCheck bool) (*types.Info, error) {
	conf := types.Config{
		Importer: &localImporter{},
		Error: func(err error) {
			if debugTypeCheck {
				fmt.Fprintf(os.Stderr, "Warning while type checking '%s': %s\n", path, err)
			}
			// Our current logic type checks only the types that are used in the rules, and "ignores" the rest.
			// It means that we expect many errors in the type checking process (missing types, undefined variables, etc).
			// In theory, we would like to log only errors that are related to the types that we support, in order to spot potential issues.
			// In practise, the message is often not enough to determine if the error is relevant or not.
			// Therefore, we don't log any error at the moment.
		},
	}

	info := &types.Info{
		Types:        make(map[ast.Expr]types.TypeAndValue),
		Defs:         make(map[*ast.Ident]types.Object),
		Uses:         make(map[*ast.Ident]types.Object),
		Implicits:    make(map[ast.Node]types.Object),
		Selections:   make(map[*ast.SelectorExpr]*types.Selection),
		Scopes:       make(map[ast.Node]*types.Scope),
		InitOrder:    []*types.Initializer{},
		Instances:    make(map[*ast.Ident]types.Instance),
		FileVersions: make(map[*ast.File]string),
	}

	_, err := conf.Check(path, fileSet, []*ast.File{astFile}, info)

	return info, err
}

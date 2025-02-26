package main

import (
	"embed"
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"golang.org/x/tools/go/gcexportdata"
	"io/fs"
	"os"
)

// PackageExportDataDir is also hardcoded in the go:embed directive below.
const PackageExportDataDir = "packages"

var (
	// This compiler directive embeds the package export data (i.e. the "packages" directory) into the Go executable
	//go:embed packages/*
	packages embed.FS

	packageExportData = map[string]string{
		"crypto":                               "crypto.o",
		"database/sql":                         "database_sql.o",
		"github.com/beego/beego/v2/server/web": "beego_server_web_v2.o",
		"github.com/emersion/go-smtp":          "emersion_smtp.o",
		"github.com/gin-gonic/gin":             "gin.o",
		"github.com/gofiber/fiber/v2":          "fiber_v2.o",
		"golang.org/x/crypto/md4":              "x_crypto_md4.o",
		"golang.org/x/crypto/ripemd160":        "x_crypto_ripemd160.o",
		"golang.org/x/sys/unix":                "x_sys_unix.o",
		"math/rand":                            "math_rand.o",
		"net/http":                             "net_http.o",
		"net/smtp":                             "net_smtp.o",
		"os":                                   "os.o",
		"runtime/debug":                        "runtime_debug.o",
		"runtime/pprof":                        "runtime_pprof.o",
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

func typeCheckAst(path string, fileSet *token.FileSet, astFile *ast.File) (*types.Info, error) {
	conf := types.Config{
		Importer: &localImporter{},
		Error: func(err error) {
			fmt.Fprintf(os.Stderr, "Error while type checking the AST: %s\n", err)
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

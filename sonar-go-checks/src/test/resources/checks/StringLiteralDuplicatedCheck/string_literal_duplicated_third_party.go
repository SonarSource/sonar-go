package StringLiteralDuplicatedCheck

import (
  apexlog "github.com/apex/log"
  gklog "github.com/go-kit/log"
  gklogLevel "github.com/go-kit/log/level"
  "github.com/golang/glog"
  pkgerrors "github.com/pkg/errors"
  "github.com/rs/zerolog"
  "github.com/sirupsen/logrus"
  "go.uber.org/zap"
  "golang.org/x/xerrors"
  "gopkg.in/inconshreveable/log15.v2"
  klog "k8s.io/klog"
  klogv2 "k8s.io/klog/v2"
)

func logrusLogging() {
  logrus.Info("logrus: operation failed")
  logrus.Info("logrus: operation failed")
  logrus.Info("logrus: operation failed") // Compliant - string literal used in logrus function

  logrus.Trace("logrus trace: operation failed")
  logrus.Trace("logrus trace: operation failed")
  logrus.Trace("logrus trace: operation failed") // Compliant - string literal used in logrus Trace function

  logrus.Error("logrus: operation failed")
  logrus.Error("logrus: operation failed")
  logrus.Error("logrus: operation failed") // Compliant - string literal used in logrus function
}

func zapLogging() {
  logger, _ := zap.NewProduction()
  logger.Info("zap: operation failed")
  logger.Info("zap: operation failed")
  logger.Info("zap: operation failed") // Compliant - string literal used in zap logger method

  sugar := logger.Sugar()
  sugar.Infof("zap sugar: operation failed")
  sugar.Infof("zap sugar: operation failed")
  sugar.Infof("zap sugar: operation failed") // Compliant - string literal used in zap sugared logger method
}

func zerologLogging() {
  var logger zerolog.Logger
  logger.Info().Msg("zerolog: operation failed")
  logger.Info().Msg("zerolog: operation failed")
  logger.Info().Msg("zerolog: operation failed") // Compliant - string literal used in zerolog Msg method

  logger.Error().Msgf("zerolog: operation %s", "failed")
  logger.Error().Msgf("zerolog: operation %s", "failed")
  logger.Error().Msgf("zerolog: operation %s", "failed") // Compliant - string literal used in zerolog Msgf method
}

func glogLogging() {
  glog.Info("glog: operation failed")
  glog.Info("glog: operation failed")
  glog.Info("glog: operation failed") // Compliant - string literal used in glog function

  glog.Error("glog: operation failed")
  glog.Error("glog: operation failed")
  glog.Error("glog: operation failed") // Compliant - string literal used in glog function
}

func klogLogging() {
  klog.Info("klog: operation failed")
  klog.Info("klog: operation failed")
  klog.Info("klog: operation failed") // Compliant - string literal used in klog function
}

func klogV2Logging() {
  klogv2.Info("klog/v2: operation failed")
  klogv2.Info("klog/v2: operation failed")
  klogv2.Info("klog/v2: operation failed") // Compliant - string literal used in klog/v2 function
}

func log15Logging() {
  log15.Info("log15: operation failed")
  log15.Info("log15: operation failed")
  log15.Info("log15: operation failed") // Compliant - string literal used in log15 function

  log15.Error("log15: operation failed")
  log15.Error("log15: operation failed")
  log15.Error("log15: operation failed") // Compliant - string literal used in log15 function
}

func goKitLogging(logger gklog.Logger) {
  _ = logger.Log("msg", "go-kit: operation failed")
  _ = logger.Log("msg", "go-kit: operation failed")
  _ = logger.Log("msg", "go-kit: operation failed") // Compliant - string literal used in go-kit/log Log method

  _ = gklogLevel.Info(logger).Log("msg", "go-kit level: operation failed")
  _ = gklogLevel.Info(logger).Log("msg", "go-kit level: operation failed")
  _ = gklogLevel.Info(logger).Log("msg", "go-kit level: operation failed") // Compliant - string literal used in go-kit/log/level logger
}

func apexLogging() {
  apexlog.Info("apex: operation failed")
  apexlog.Info("apex: operation failed")
  apexlog.Info("apex: operation failed") // Compliant - string literal used in apex/log function

  apexlog.Error("apex: operation failed")
  apexlog.Error("apex: operation failed")
  apexlog.Error("apex: operation failed") // Compliant - string literal used in apex/log function
}

func xerrorsUsage() {
  _ = xerrors.Errorf("xerrors: operation failed")
  _ = xerrors.Errorf("xerrors: operation failed")
  _ = xerrors.Errorf("xerrors: operation failed") // Compliant - string literal used in xerrors.Errorf

  _ = xerrors.New("xerrors: invalid input")
  _ = xerrors.New("xerrors: invalid input")
  _ = xerrors.New("xerrors: invalid input") // Compliant - string literal used in xerrors.New
}

func pkgErrorsUsage() {
  _ = pkgerrors.New("pkg/errors: operation failed")
  _ = pkgerrors.New("pkg/errors: operation failed")
  _ = pkgerrors.New("pkg/errors: operation failed") // Compliant - string literal used in pkg/errors New

  _ = pkgerrors.Errorf("pkg/errors: operation failed")
  _ = pkgerrors.Errorf("pkg/errors: operation failed")
  _ = pkgerrors.Errorf("pkg/errors: operation failed") // Compliant - string literal used in pkg/errors Errorf

  _ = pkgerrors.Wrap(nil, "pkg/errors: operation failed")
  _ = pkgerrors.Wrap(nil, "pkg/errors: operation failed")
  _ = pkgerrors.Wrap(nil, "pkg/errors: operation failed") // Compliant - string literal used in pkg/errors Wrap
}

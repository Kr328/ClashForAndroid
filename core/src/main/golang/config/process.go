package config

import (
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/Dreamacro/clash/log"
	"github.com/dlclark/regexp2"

	"cfa/app"
	"cfa/common"

	"github.com/Dreamacro/clash/config"
	"github.com/Dreamacro/clash/dns"
)

const (
	defaultHealthCheckUrl      = "https://www.gstatic.com/generate_204"
	defaultHealthCheckInterval = time.Hour
)

var processors = []processor{
	patchOverride,
	patchGeneral,
	patchProfile,
	patchDns,
	patchProviders,
	patchProxyGroup,
	validConfig,
}

type processor func(cfg *config.RawConfig, profileDir string) error

func patchOverride(cfg *config.RawConfig, _ string) error {
	if err := json.NewDecoder(strings.NewReader(ReadOverride(OverrideSlotPersist))).Decode(cfg); err != nil {
		log.Warnln("Apply persist override: %s", err.Error())
	}
	if err := json.NewDecoder(strings.NewReader(ReadOverride(OverrideSlotSession))).Decode(cfg); err != nil {
		log.Warnln("Apply session override: %s", err.Error())
	}

	return nil
}

func patchGeneral(cfg *config.RawConfig, _ string) error {
	cfg.Interface = ""
	cfg.ExternalUI = ""
	cfg.ExternalController = ""

	return nil
}

func patchProfile(cfg *config.RawConfig, _ string) error {
	cfg.Profile.StoreSelected = false

	return nil
}

func patchDns(cfg *config.RawConfig, _ string) error {
	if !cfg.DNS.Enable {
		cfg.DNS.Enable = true
		cfg.DNS.IPv6 = false
		cfg.DNS.NameServer = defaultNameServers
		cfg.DNS.Fallback = defaultFallback
		cfg.DNS.FallbackFilter.GeoIP = false
		cfg.DNS.FallbackFilter.IPCIDR = localNetwork
		cfg.DNS.EnhancedMode = dns.MAPPING
		cfg.DNS.FakeIPRange = "198.18.0.0/16"
		cfg.DNS.DefaultNameserver = defaultNameServers
		cfg.DNS.FakeIPFilter = defaultFakeIPFilter

		cfg.ClashForAndroid.AppendSystemDNS = true
	}

	if cfg.ClashForAndroid.AppendSystemDNS {
		cfg.DNS.NameServer = append(cfg.DNS.NameServer, app.SystemDns()...)
	}

	return nil
}

func patchProviders(cfg *config.RawConfig, profileDir string) error {
	forEachProviders(cfg, func(index int, total int, key string, provider map[string]interface{}) {
		if path, ok := provider["path"].(string); ok {
			provider["path"] = profileDir + "/providers/" + common.ResolveAsRoot(path)
		}
	})

	return nil
}

func patchProxyGroup(cfg *config.RawConfig, _ string) error {
	for _, g := range cfg.ProxyGroup {
		if _, exist := g["url"]; !exist {
			g["url"] = defaultHealthCheckUrl
		}

		if _, exist := g["interval"]; !exist {
			g["interval"] = int(defaultHealthCheckInterval.Seconds())
		}
	}

	return nil
}

func validConfig(cfg *config.RawConfig, _ string) error {
	if len(cfg.Proxy) == 0 && len(cfg.ProxyProvider) == 0 {
		return errors.New("profile does not contain `proxies` or `proxy-providers`")
	}

	if _, err := regexp2.Compile(cfg.ClashForAndroid.UiSubtitlePattern, 0); err != nil {
		return fmt.Errorf("compile ui-subtitle-pattern: %s", err.Error())
	}

	return nil
}

func process(cfg *config.RawConfig, profileDir string) error {
	for _, p := range processors {
		if err := p(cfg, profileDir); err != nil {
			return err
		}
	}

	return nil
}

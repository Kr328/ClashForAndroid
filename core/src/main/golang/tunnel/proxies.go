package tunnel

import (
	"sort"
	"strings"

	"github.com/Dreamacro/clash/adapter"
	"github.com/dlclark/regexp2"

	"github.com/Dreamacro/clash/adapter/outboundgroup"
	"github.com/Dreamacro/clash/adapter/provider"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

type SortMode int

const (
	Default SortMode = iota
	Title
	Delay
)

type Proxy struct {
	Name     string `json:"name"`
	Title    string `json:"title"`
	Subtitle string `json:"subtitle"`
	Type     string `json:"type"`
	Delay    int    `json:"delay"`
}

type ProxyGroup struct {
	Type    string   `json:"type"`
	Now     string   `json:"now"`
	Proxies []*Proxy `json:"proxies"`
}

type sortableProxyList struct {
	list []*Proxy
	less func(a, b *Proxy) bool
}

func (s *sortableProxyList) Len() int {
	return len(s.list)
}

func (s *sortableProxyList) Less(i, j int) bool {
	return s.less(s.list[i], s.list[j])
}

func (s *sortableProxyList) Swap(i, j int) {
	s.list[i], s.list[j] = s.list[j], s.list[i]
}

func QueryProxyGroupNames(excludeNotSelectable bool) []string {
	mode := tunnel.Mode()

	if mode == tunnel.Direct {
		return []string{}
	}

	global := tunnel.Proxies()["GLOBAL"].(*adapter.Proxy).ProxyAdapter.(outboundgroup.ProxyGroup)
	proxies := global.Providers()[0].Proxies()
	result := make([]string, 0, len(proxies)+1)

	if mode == tunnel.Global {
		result = append(result, "GLOBAL")
	}

	for _, p := range proxies {
		if _, ok := p.(*adapter.Proxy).ProxyAdapter.(outboundgroup.ProxyGroup); ok {
			if !excludeNotSelectable || p.Type() == C.Selector {
				result = append(result, p.Name())
			}
		}
	}

	return result
}

func QueryProxyGroup(name string, sortMode SortMode, uiSubtitlePattern *regexp2.Regexp) *ProxyGroup {
	p := tunnel.Proxies()[name]

	if p == nil {
		log.Warnln("Query group `%s`: not found", name)

		return nil
	}

	g, ok := p.(*adapter.Proxy).ProxyAdapter.(outboundgroup.ProxyGroup)
	if !ok {
		log.Warnln("Query group `%s`: invalid type %s", name, p.Type().String())

		return nil
	}

	proxies := collectProviders(g.Providers(), uiSubtitlePattern)

	switch sortMode {
	case Title:
		wrapper := &sortableProxyList{
			list: proxies,
			less: func(a, b *Proxy) bool {
				return strings.Compare(a.Title, b.Title) < 0
			},
		}

		sort.Sort(wrapper)
	case Delay:
		wrapper := &sortableProxyList{
			list: proxies,
			less: func(a, b *Proxy) bool {
				return a.Delay < b.Delay
			},
		}

		sort.Sort(wrapper)
	case Default:
	default:
	}

	return &ProxyGroup{
		Type:    g.Type().String(),
		Now:     g.Now(),
		Proxies: proxies,
	}
}

func PatchSelector(selector, name string) bool {
	p := tunnel.Proxies()[selector]

	if p == nil {
		log.Warnln("Patch selector `%s`: not found", selector)

		return false
	}

	g, ok := p.(*adapter.Proxy).ProxyAdapter.(outboundgroup.ProxyGroup)
	if !ok {
		log.Warnln("Patch selector `%s`: invalid type %s", selector, p.Type().String())

		return false
	}

	s, ok := g.(*outboundgroup.Selector)
	if !ok {
		log.Warnln("Patch selector `%s`: invalid type %s", selector, p.Type().String())

		return false
	}

	if err := s.Set(name); err != nil {
		log.Warnln("Patch selector `%s`: %s", selector, err.Error())
	}

	log.Infoln("Patch selector %s -> %s", selector, name)

	closeConnByGroup(selector)

	return true
}

func collectProviders(providers []provider.ProxyProvider, uiSubtitlePattern *regexp2.Regexp) []*Proxy {
	result := make([]*Proxy, 0, 128)

	for _, p := range providers {
		for _, px := range p.Proxies() {
			name := px.Name()
			title := name
			subtitle := px.Type().String()

			if uiSubtitlePattern != nil {
				if _, ok := px.(*adapter.Proxy).ProxyAdapter.(outboundgroup.ProxyGroup); !ok {
					runes := []rune(name)
					match, err := uiSubtitlePattern.FindRunesMatch(runes)
					if err == nil && match != nil {
						title = string(runes[:match.Index]) + string(runes[match.Index+match.Length:])
						subtitle = string(runes[match.Index : match.Index+match.Length])
					}
				}
			}

			result = append(result, &Proxy{
				Name:     name,
				Title:    strings.TrimSpace(title),
				Subtitle: strings.TrimSpace(subtitle),
				Type:     px.Type().String(),
				Delay:    int(px.LastDelay()),
			})
		}
	}

	return result
}

// +build !premium

package tunnel

import (
	"errors"
	"fmt"
	"time"

	"github.com/Dreamacro/clash/adapter/provider"
	"github.com/Dreamacro/clash/tunnel"
)

var ErrInvalidType = errors.New("invalid type")

type Provider struct {
	Name        string `json:"name"`
	VehicleType string `json:"vehicleType"`
	Type        string `json:"type"`
	UpdatedAt   int64  `json:"updatedAt"`
}

func QueryProviders() []*Provider {
	p := tunnel.Providers()

	providers := make([]provider.Provider, 0, len(p))

	for _, proxy := range p {
		if proxy.VehicleType() == provider.Compatible {
			continue
		}

		providers = append(providers, proxy)
	}

	result := make([]*Provider, 0, len(providers))

	for _, p := range providers {
		updatedAt := time.Time{}

		if s, ok := p.(provider.UpdatableProvider); ok {
			updatedAt = s.UpdatedAt()
		}

		result = append(result, &Provider{
			Name:        p.Name(),
			VehicleType: p.VehicleType().String(),
			Type:        p.Type().String(),
			UpdatedAt:   updatedAt.UnixNano() / 1000 / 1000,
		})
	}

	return result
}

func UpdateProvider(_ string, name string) error {
	p, ok := tunnel.Providers()[name]
	if !ok {
		return fmt.Errorf("%s not found", name)
	}

	return p.Update()
}

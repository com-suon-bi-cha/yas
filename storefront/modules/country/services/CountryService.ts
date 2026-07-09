import { Country } from '../models/Country';
import apiClientService from '@/common/services/ApiClientService';

export async function getCountries(): Promise<Country[]> {
  const response = await apiClientService.get(`/api/location/storefront/countries`);
  if (response.status === 204) {
    return [];
  }
  const countries: Country[] = await response.json();
  return countries.filter((country) => country.code2?.trim() === 'VN');
}

<?php

/**
 * This code was generated by
 * \ / _    _  _|   _  _
 * | (_)\/(_)(_|\/| |(/_  v1.0.0
 * /       /
 */

namespace Twilio\Rest\Verify\V2\Service;

use Twilio\Exceptions\TwilioException;
use Twilio\ListResource;
use Twilio\Values;
use Twilio\Version;

/**
 * PLEASE NOTE that this class contains beta products that are subject to change. Use them with caution.
 */
class MessagingConfigurationList extends ListResource {
    /**
     * Construct the MessagingConfigurationList
     *
     * @param Version $version Version that contains the resource
     * @param string $serviceSid The SID of the Service that the resource is
     *                           associated with
     * @return \Twilio\Rest\Verify\V2\Service\MessagingConfigurationList
     */
    public function __construct(Version $version, $serviceSid) {
        parent::__construct($version);

        // Path Solution
        $this->solution = array('serviceSid' => $serviceSid, );

        $this->uri = '/Services/' . \rawurlencode($serviceSid) . '/MessagingConfigurations';
    }

    /**
     * Create a new MessagingConfigurationInstance
     *
     * @param string $country The ISO-3166-1 country code of the country or `all`.
     * @param string $messagingServiceSid The SID of the Messaging Service used for
     *                                    this configuration.
     * @return MessagingConfigurationInstance Newly created
     *                                        MessagingConfigurationInstance
     * @throws TwilioException When an HTTP error occurs.
     */
    public function create($country, $messagingServiceSid) {
        $data = Values::of(array('Country' => $country, 'MessagingServiceSid' => $messagingServiceSid, ));

        $payload = $this->version->create(
            'POST',
            $this->uri,
            array(),
            $data
        );

        return new MessagingConfigurationInstance($this->version, $payload, $this->solution['serviceSid']);
    }

    /**
     * Streams MessagingConfigurationInstance records from the API as a generator
     * stream.
     * This operation lazily loads records as efficiently as possible until the
     * limit
     * is reached.
     * The results are returned as a generator, so this operation is memory
     * efficient.
     *
     * @param int $limit Upper limit for the number of records to return. stream()
     *                   guarantees to never return more than limit.  Default is no
     *                   limit
     * @param mixed $pageSize Number of records to fetch per request, when not set
     *                        will use the default value of 50 records.  If no
     *                        page_size is defined but a limit is defined, stream()
     *                        will attempt to read the limit with the most
     *                        efficient page size, i.e. min(limit, 1000)
     * @return \Twilio\Stream stream of results
     */
    public function stream($limit = null, $pageSize = null) {
        $limits = $this->version->readLimits($limit, $pageSize);

        $page = $this->page($limits['pageSize']);

        return $this->version->stream($page, $limits['limit'], $limits['pageLimit']);
    }

    /**
     * Reads MessagingConfigurationInstance records from the API as a list.
     * Unlike stream(), this operation is eager and will load `limit` records into
     * memory before returning.
     *
     * @param int $limit Upper limit for the number of records to return. read()
     *                   guarantees to never return more than limit.  Default is no
     *                   limit
     * @param mixed $pageSize Number of records to fetch per request, when not set
     *                        will use the default value of 50 records.  If no
     *                        page_size is defined but a limit is defined, read()
     *                        will attempt to read the limit with the most
     *                        efficient page size, i.e. min(limit, 1000)
     * @return MessagingConfigurationInstance[] Array of results
     */
    public function read($limit = null, $pageSize = null) {
        return \iterator_to_array($this->stream($limit, $pageSize), false);
    }

    /**
     * Retrieve a single page of MessagingConfigurationInstance records from the
     * API.
     * Request is executed immediately
     *
     * @param mixed $pageSize Number of records to return, defaults to 50
     * @param string $pageToken PageToken provided by the API
     * @param mixed $pageNumber Page Number, this value is simply for client state
     * @return \Twilio\Page Page of MessagingConfigurationInstance
     */
    public function page($pageSize = Values::NONE, $pageToken = Values::NONE, $pageNumber = Values::NONE) {
        $params = Values::of(array(
            'PageToken' => $pageToken,
            'Page' => $pageNumber,
            'PageSize' => $pageSize,
        ));

        $response = $this->version->page(
            'GET',
            $this->uri,
            $params
        );

        return new MessagingConfigurationPage($this->version, $response, $this->solution);
    }

    /**
     * Retrieve a specific page of MessagingConfigurationInstance records from the
     * API.
     * Request is executed immediately
     *
     * @param string $targetUrl API-generated URL for the requested results page
     * @return \Twilio\Page Page of MessagingConfigurationInstance
     */
    public function getPage($targetUrl) {
        $response = $this->version->getDomain()->getClient()->request(
            'GET',
            $targetUrl
        );

        return new MessagingConfigurationPage($this->version, $response, $this->solution);
    }

    /**
     * Constructs a MessagingConfigurationContext
     *
     * @param string $country The ISO-3166-1 country code of the country or `all`.
     * @return \Twilio\Rest\Verify\V2\Service\MessagingConfigurationContext
     */
    public function getContext($country) {
        return new MessagingConfigurationContext($this->version, $this->solution['serviceSid'], $country);
    }

    /**
     * Provide a friendly representation
     *
     * @return string Machine friendly representation
     */
    public function __toString() {
        return '[Twilio.Verify.V2.MessagingConfigurationList]';
    }
}